//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford, formerly University of Birmingham)
//	
//------------------------------------------------------------------------------
//	
//	This file is part of PRISM.
//	
//	PRISM is free software; you can redistribute it and/or modify
//	it under the terms of the GNU General Public License as published by
//	the Free Software Foundation; either version 2 of the License, or
//	(at your option) any later version.
//	
//	PRISM is distributed in the hope that it will be useful,
//	but WITHOUT ANY WARRANTY; without even the implied warranty of
//	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//	GNU General Public License for more details.
//	
//	You should have received a copy of the GNU General Public License
//	along with PRISM; if not, write to the Free Software Foundation,
//	Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//	
//==============================================================================

// includes
#include "PrismHybrid.h"
#include <math.h>
#include <util.h>
#include <cudd.h>
#include <dd.h>
#include <odd.h>
#include <dv.h>
#include <prism.h>
#include "sparse.h"
#include "hybrid.h"
#include "PrismHybridGlob.h"
#include "jnipointer.h"

// local prototypes
static void mult_rec(HDDNode *hdd, int level, int row_offset, int col_offset);
static void mult_cm(CMSparseMatrix *cmsm, int row_offset, int col_offset);
static void mult_cmsc(CMSCSparseMatrix *cmscsm, int row_offset, int col_offset);

// globals (used by local functions)
static HDDNode *zero;
static int num_levels;
static bool compact_sm;
static double *sm_dist;
static int sm_dist_shift;
static int sm_dist_mask;
static double *soln, *soln2;
static double unif;

//------------------------------------------------------------------------------

JNIEXPORT jlong __pointer JNICALL Java_hybrid_PrismHybrid_PH_1StochTransient
(
JNIEnv *env,
jclass cls,
jlong __pointer tr,	// trans matrix
jlong __pointer od,	// odd
jlong __pointer in,	// initial distribution
jlong __pointer rv,	// row vars
jint num_rvars,
jlong __pointer cv,	// col vars
jint num_cvars,
jdouble time		// time bound
)
{
	// cast function parameters
	DdNode *trans = jlong_to_DdNode(tr);		// trans matrix
	ODDNode *odd = jlong_to_ODDNode(od);		// odd
	DdNode *init = jlong_to_DdNode(in);		// initial distribution
	DdNode **rvars = jlong_to_DdNode_array(rv);	// row vars
	DdNode **cvars = jlong_to_DdNode_array(cv);	// col vars

	// model stats
	int n;
	// flags
	bool compact_d;
	// matrix mtbdd
	HDDMatrix *hddm;
	HDDNode *hdd;
	// vectors
	double *diags, *tmpsoln, *sum;
	DistVector *diags_dist;
	// fox glynn stuff
	FoxGlynnWeights fgw;
	// timing stuff
	long start1, start2, start3, stop;
	double time_taken, time_for_setup, time_for_iters;
	// misc
	bool done;
	int i, j, iters, num_iters;
	double kb, kbt, max_diag, weight, term_crit_param_unif;
	
	// start clocks	
	start1 = start2 = util_cpu_time();
	
	// get number of states from odd
	n = odd->eoff + odd->toff;
	
	// build hdd for matrix
	PH_PrintToMainLog(env, "\nBuilding hybrid MTBDD matrix... ");
	hddm = build_hdd_matrix(trans, rvars, cvars, num_rvars, odd, false);
	hdd = hddm->top;
	zero = hddm->zero;
	num_levels = hddm->num_levels;
	kb = hddm->mem_nodes;
	kbt = kb;
	PH_PrintToMainLog(env, "[levels=%d, nodes=%d] [%.1f KB]\n", hddm->num_levels, hddm->num_nodes, kb);
	
	// add sparse matrices
	PH_PrintToMainLog(env, "Adding explicit sparse matrices... ");
	add_sparse_matrices(hddm, compact, false);
	compact_sm = hddm->compact_sm;
	if (compact_sm) {
		sm_dist = hddm->dist;
		sm_dist_shift = hddm->dist_shift;
		sm_dist_mask = hddm->dist_mask;
	}
	kb = hddm->mem_sm;
	kbt += kb;
	PH_PrintToMainLog(env, "[levels=%d, num=%d%s] [%.1f KB]\n", hddm->l_sm, hddm->num_sm, compact_sm?", compact":"", kb);
	
	// get vector of diagonals
	PH_PrintToMainLog(env, "Creating vector for diagonals... ");
	diags = hdd_negative_row_sums(hddm, n);
	compact_d = false;
	// try and convert to compact form if required
	if (compact) {
		if (diags_dist = double_vector_to_dist(diags, n)) {
			compact_d = true;
			free(diags);
		}
	}
	kb = (!compact_d) ? n*8.0/1024.0 : (diags_dist->num_dist*8.0+n*2.0)/1024.0;
	kbt += kb;
	if (!compact_d) PH_PrintToMainLog(env, "[%.1f KB]\n", kb);
	else PH_PrintToMainLog(env, "[dist=%d, compact] [%.1f KB]\n", diags_dist->num_dist, kb);
	//for(i = 0; i < n; i++) printf("%f ", (!compact_d)?(diags[i]):(diags_dist->dist[diags_dist->ptrs[i]])); printf("\n");
	
	// find max diagonal element
	if (!compact_d) {
		max_diag = diags[0];
		for (i = 1; i < n; i++) if (diags[i] < max_diag) max_diag = diags[i];
	} else {
		max_diag = diags_dist->dist[0];
		for (i = 1; i < diags_dist->num_dist; i++) if (diags_dist->dist[i] < max_diag) max_diag = diags_dist->dist[i];
	}
	max_diag = -max_diag;
	
	// constant for uniformization
	unif = 1.02*max_diag;
	last_unif = unif;
	
	// modify diagonals
	if (!compact_d) {
		for (i = 0; i < n; i++) diags[i] = diags[i] / unif + 1;
	} else {
		for (i = 0; i < diags_dist->num_dist; i++) diags_dist->dist[i] = diags_dist->dist[i] / unif + 1;
	}
	
	// create solution/iteration vectors
	PH_PrintToMainLog(env, "Allocating iteration vectors... ");
	soln = mtbdd_to_double_vector(ddman, init, rvars, num_rvars, odd);
	soln2 = new double[n];
	sum = new double[n];
	kb = n*8.0/1024.0;
	kbt += 3*kb;
	PH_PrintToMainLog(env, "[3 x %.1f KB]\n", kb);
	
	// print total memory usage
	PH_PrintToMainLog(env, "TOTAL: [%.1f KB]\n", kbt);
	
	// compute new termination criterion parameter (epsilon/8)
	term_crit_param_unif = term_crit_param / 8.0;
	
	// compute poisson probabilities (fox/glynn)
	PH_PrintToMainLog(env, "\nUniformisation: q.t = %f x %f = %f\n", unif, time, unif * time);
	fgw = fox_glynn(unif * time, 1.0e-300, 1.0e+300, term_crit_param_unif);
	for (i = fgw.left; i <= fgw.right; i++) {
		fgw.weights[i-fgw.left] /= fgw.total_weight;
	}
	PH_PrintToMainLog(env, "Fox-Glynn: left = %d, right = %d\n", fgw.left, fgw.right);
	
	// set up vectors
	for (i = 0; i < n; i++) {
		sum[i] = 0.0;
	}
	
	// get setup time
	stop = util_cpu_time();
	time_for_setup = (double)(stop - start2)/1000;
	start2 = stop;
	
	// start transient analysis
	done = false;
	num_iters = -1;
	PH_PrintToMainLog(env, "\nStarting iterations...\n");
	
	// if necessary, do 0th element of summation (doesn't require any matrix powers)
	if (fgw.left == 0) for (i = 0; i < n; i++) {
		sum[i] += fgw.weights[0] * soln[i];
	}
	
	// note that we ignore max_iters as we know how any iterations _should_ be performed
	for (iters = 1; (iters <= fgw.right) && !done; iters++) {
		
//		PH_PrintToMainLog(env, "Iteration %d: ", iters);
//		start3 = util_cpu_time();
		
		// initialise vector
		if (!compact_d) {
			for (i = 0; i < n; i++) soln2[i] = diags[i] * soln[i];
		} else {
			for (i = 0; i < n; i++) soln2[i] = diags_dist->dist[diags_dist->ptrs[i]] * soln[i];
		}
		
		// do matrix vector multiply bit
		mult_rec(hdd, 0, 0, 0);
		
		// check for steady state convergence
		if (do_ss_detect) switch (term_crit) {
		case TERM_CRIT_ABSOLUTE:
			done = true;
			for (i = 0; i < n; i++) {
				if (fabs(soln2[i] - soln[i]) > term_crit_param_unif) {
					done = false;
					break;
				}
			}
			break;
		case TERM_CRIT_RELATIVE:
			done = true;
			for (i = 0; i < n; i++) {
				if (fabs((soln2[i] - soln[i])/soln2[i]) > term_crit_param_unif) {
					done = false;
					break;
				}
			}
			break;
		}
		
		// special case when finished early (steady-state detected)
		if (done) {
			// work out sum of remaining poisson probabilities
			if (iters <= fgw.left) {
				weight = 1.0;
			} else {
				weight = 0.0;
				for (i = iters; i <= fgw.right; i++) {
					weight += fgw.weights[i-fgw.left];
				}
			}
			// add to sum
			for (i = 0; i < n; i++) sum[i] += weight * soln2[i];
			PH_PrintToMainLog(env, "\nSteady state detected at iteration %d\n", iters);
			num_iters = iters;
			break;
		}
		
		// prepare for next iteration
		tmpsoln = soln;
		soln = soln2;
		soln2 = tmpsoln;
		
		// add to sum
		if (iters >= fgw.left) {
			for (i = 0; i < n; i++) sum[i] += fgw.weights[iters-fgw.left] * soln[i];
		}
		
//		PH_PrintToMainLog(env, "%.2f %.2f sec\n", ((double)(util_cpu_time() - start3)/1000), ((double)(util_cpu_time() - start2)/1000)/iters);
	}
	
	// stop clocks
	stop = util_cpu_time();
	time_for_iters = (double)(stop - start2)/1000;
	time_taken = (double)(stop - start1)/1000;
	
	// print iters/timing info
	if (num_iters == -1) num_iters = fgw.right;
	PH_PrintToMainLog(env, "\nIterative method: %d iterations in %.2f seconds (average %.6f, setup %.2f)\n", num_iters, time_taken, time_for_iters/num_iters, time_for_setup);
	
	// free memory
	free_hdd_matrix(hddm);
	if (compact_d) free_dist_vector(diags_dist); else free(diags);
	delete soln;
	delete soln2;
	
	return ptr_to_jlong(sum);
}

//------------------------------------------------------------------------------

static void mult_rec(HDDNode *hdd, int level, int row_offset, int col_offset)
{
	HDDNode *e, *t;
	
	// if it's the zero node
	if (hdd == zero) {
		return;
	}
	// or if we've reached a submatrix
	// (check for non-null ptr but, equivalently, we could just check if level==l_sm)
	else if (hdd->sm.ptr) {
		if (!compact_sm) {
			mult_cm((CMSparseMatrix *)hdd->sm.ptr, row_offset, col_offset);
		} else {
			mult_cmsc((CMSCSparseMatrix *)hdd->sm.ptr, row_offset, col_offset);
		}
		return;
	}
	// or if we've reached the bottom
	else if (level == num_levels) {
		//printf("(%d,%d)=%f\n", col_offset, row_offset, hdd->type.val);
		soln2[col_offset] += soln[row_offset] * (hdd->type.val / unif);
		return;
	}
	// otherwise recurse
	e = hdd->type.kids.e;
	if (e != zero) {
		mult_rec(e->type.kids.e, level+1, row_offset, col_offset);
		mult_rec(e->type.kids.t, level+1, row_offset, col_offset+e->off.val);
	}
	t = hdd->type.kids.t;
	if (t != zero) {
		mult_rec(t->type.kids.e, level+1, row_offset+hdd->off.val, col_offset);
		mult_rec(t->type.kids.t, level+1, row_offset+hdd->off.val, col_offset+t->off.val);
	}
}

//-----------------------------------------------------------------------------------

static void mult_cm(CMSparseMatrix *cmsm, int row_offset, int col_offset)
{
	int i2, j2, l2, h2;
	int sm_n = cmsm->n;
	int sm_nnz = cmsm->nnz;
	double *sm_non_zeros = cmsm->non_zeros;
	unsigned char *sm_col_counts = cmsm->col_counts;
	int *sm_col_starts = (int *)cmsm->col_counts;
	bool sm_use_counts = cmsm->use_counts;
	unsigned int *sm_rows = cmsm->rows;
	
	// loop through columns of submatrix
	l2 = sm_nnz; h2 = 0;
	for (i2 = 0; i2 < sm_n; i2++) {
		
		// loop through entries in this column
		if (!sm_use_counts) { l2 = sm_col_starts[i2]; h2 = sm_col_starts[i2+1]; }
		else { l2 = h2; h2 += sm_col_counts[i2]; }
		for (j2 = l2; j2 < h2; j2++) {
			soln2[col_offset + i2] += soln[row_offset + sm_rows[j2]] * (sm_non_zeros[j2] / unif);
			//printf("(%d,%d)=%f\n", col_offset + sm_rows[j2], row_offset + i2, sm_non_zeros[j2]);
		}
	}
}

//-----------------------------------------------------------------------------------

static void mult_cmsc(CMSCSparseMatrix *cmscsm, int row_offset, int col_offset)
{
	int i2, j2, l2, h2;
	int sm_n = cmscsm->n;
	int sm_nnz = cmscsm->nnz;
	unsigned char *sm_col_counts = cmscsm->col_counts;
	int *sm_col_starts = (int *)cmscsm->col_counts;
	bool sm_use_counts = cmscsm->use_counts;
	unsigned int *sm_rows = cmscsm->rows;
	
	// loop through columns of submatrix
	l2 = sm_nnz; h2 = 0;
	for (i2 = 0; i2 < sm_n; i2++) {
		
		// loop through entries in this column
		if (!sm_use_counts) { l2 = sm_col_starts[i2]; h2 = sm_col_starts[i2+1]; }
		else { l2 = h2; h2 += sm_col_counts[i2]; }
		for (j2 = l2; j2 < h2; j2++) {
			soln2[col_offset + i2] += soln[row_offset + (int)(sm_rows[j2] >> sm_dist_shift)] * (sm_dist[(int)(sm_rows[j2] & sm_dist_mask)] / unif);
			//printf("(%d,%d)=%f\n", col_offset + (int)(sm_rows[j2] >> sm_dist_shift), row_offset + i2, sm_dist[(int)(sm_rows[j2] & sm_dist_mask)]);
		}
	}
}

//------------------------------------------------------------------------------
