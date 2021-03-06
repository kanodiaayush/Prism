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

package parser.ast;

import parser.*;
import parser.visitor.*;
import prism.PrismLangException;

public class ExpressionReward extends Expression
{
	Object rewardStructIndex = null;
	RelOp relOp = null;
	Expression reward = null;
	Expression expression = null;
	// Note: this "old-style" filter is just for display purposes
	// The parser creates an (invisible) new-style filter around this expression
	Filter filter = null;
	
	// Constructors
	
	public ExpressionReward()
	{
	}
	
	public ExpressionReward(Expression e, String r, Expression p)
	{
		expression = e;
		relOp = RelOp.parseSymbol(r);
		reward = p;
	}

	// Set methods
	
	public void setRewardStructIndex(Object o)
	{
		rewardStructIndex = o;
	}

	public void setRelOp(RelOp relOp)
	{
		this.relOp = relOp;
	}

	public void setRelOp(String r)
	{
		relOp = RelOp.parseSymbol(r);
	}

	public void setReward(Expression p)
	{
		reward = p;
	}

	public void setExpression(Expression e)
	{
		expression = e;
	}
	
	public void setFilter(Filter f)
	{
		filter = f;
	}

	// Get methods
	
	public Object getRewardStructIndex()
	{
		return rewardStructIndex;
	}

	public RelOp getRelOp()
	{
		return relOp;
	}
	
	public Expression getReward()
	{
		return reward;
	}

	public Expression getExpression()
	{
		return expression;
	}
	
	public Filter getFilter()
	{
		return filter;
	}

	// Methods required for Expression:
	
	/**
	 * Is this expression constant?
	 */
	public boolean isConstant()
	{
		return false;
	}

	@Override
	public boolean isProposition()
	{
		return false;
	}
	
	/**
	 * Evaluate this expression, return result.
	 * Note: assumes that type checking has been done already.
	 */
	public Object evaluate(EvaluateContext ec) throws PrismLangException
	{
		throw new PrismLangException("Cannot evaluate an R operator without a model");
	}

	/**
	  * Get "name" of the result of this expression (used for y-axis of any graphs plotted)
	  */
	public String getResultName()
	{
		// For R=? properties, use name of reward structure where applicable
		if (reward == null) {
			String s = "E";
			if (relOp == RelOp.MIN) s = "Minimum e";
			else if (relOp == RelOp.MAX) s = "Maximum e";
			else s = "E";
			if (rewardStructIndex instanceof String) s += "xpected "+rewardStructIndex;
			// Or just call it "Expected reward"
			else s += "xpected reward";
			return s;
		}
		// For R>r etc., just use "Result"
		else {
			return "Result";
		}
	}

	@Override
	public boolean returnsSingleValue()
	{
		return false;
	}

	// Methods required for ASTElement:
	
	/**
	 * Visitor method.
	 */
	public Object accept(ASTVisitor v) throws PrismLangException
	{
		return v.visit(this);
	}
	
	/**
	 * Convert to string.
	 */
	public String toString()
	{
		String s = "";
		
		s += "R";
		if (rewardStructIndex != null) {
			if (rewardStructIndex instanceof Expression) s += "{"+rewardStructIndex+"}";
			else if (rewardStructIndex instanceof String) s += "{\""+rewardStructIndex+"\"}";
		}
		s += relOp;
		s += (reward==null) ? "?" : reward.toString();
		s += " [ " + expression;
		if (filter != null) s += " "+filter;
		s += " ]";
		
		return s;
	}

	/**
	 * Perform a deep copy.
	 */
	public Expression deepCopy()
	{
		ExpressionReward expr = new ExpressionReward();
		expr.setExpression(expression == null ? null : expression.deepCopy());
		expr.setRelOp(relOp);
		expr.setReward(reward == null ? null : reward.deepCopy());
		if (rewardStructIndex != null && rewardStructIndex instanceof Expression) expr.setRewardStructIndex(((Expression)rewardStructIndex).deepCopy());
		else expr.setRewardStructIndex(rewardStructIndex);
		expr.setFilter(filter == null ? null : (Filter)filter.deepCopy());
		expr.setType(type);
		expr.setPosition(this);
		return expr;
	}
}

//------------------------------------------------------------------------------
