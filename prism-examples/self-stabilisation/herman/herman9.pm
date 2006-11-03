// herman's self stabilising algorithm [Her90]
// gxn/dxp 13/07/02

// the procotol is synchronous with no non-determinism (a DTMC)
dtmc

// module for process 1
module process1

	// Boolean variable for process 1
	x1 : [0..1];
	
	[step]  (x1=x9) -> 0.5 : (x1'=0) + 0.5 : (x1'=1);
	[step] !(x1=x9) -> (x1'=x9);
	
endmodule

// add further processes through renaming
module process2 = process1[x1=x2, x9=x1 ] endmodule
module process3 = process1[x1=x3, x9=x2 ] endmodule
module process4 = process1[x1=x4, x9=x3 ] endmodule
module process5 = process1[x1=x5, x9=x4 ] endmodule
module process6 = process1[x1=x6, x9=x5 ] endmodule
module process7 = process1[x1=x7, x9=x6 ] endmodule
module process8 = process1[x1=x8, x9=x7 ] endmodule
module process9 = process1[x1=x9, x9=x8 ] endmodule

// cost - 1 in each state (expected steps)
rewards
	
	true : 1;
	
endrewards

// any initial state (consider any possible initial configuration of tokens)
init
	
	true
	
endinit
