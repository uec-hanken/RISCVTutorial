`timescale 1ns / 1ps
//---------------------------------------------------------------------------
//
// gcd(x,y)}
// GCD = Greatest Common Divisor
// The GCD of two or more integers 
// is the largest positive integer that divides each of the integers
// 
// Euclid's algorithm
//		c=gcd(a,b)
//		BEGIN
//			while(b!=0); do
//				if(a>b) swap(a,b)
//				else b=b-a
//			done
//			c=a
//		END
//
//---------------------------------------------------------------------------

module GCD(
	input iClk,
	input iRst,
    input iValid,
	input [15:0] iA,
	input [15:0] iB,
	output reg oValid,
	output reg oReady,
	output reg [15:0] oC
	);
	
	parameter [1:0] s_Idle = 2'b00, s_Calculate = 2'b01, s_Done = 2'b10;
	reg [1:0] state;
	reg [15:0] regA, regB;
	
	// process_state_Machine
	always @(posedge iClk)
	begin	
		if (iRst == 1) begin
			state <= s_Idle;
			oReady <= 1;
			oValid <= 0;				
		end else begin
			case(state)
			//----------------------- s_Idle
			s_Idle:
				begin
					oReady <= 1;
					oValid <= 0;
					if (iValid == 1) begin
						regA <= iA;
						regB <= iB;
						oReady <= 0;
						state <= s_Calculate;
					end else
						state <= s_Idle;
				end
					
			//----------------------- s_Calculate
			s_Calculate:
				begin
					if (regB != 0) begin						
						if (regA > regB)
							begin
								regB <= regA;
								regA <= regB;
							end
						else
							regB <= regB - regA;
														
						state <= s_Calculate;
					end else begin
						state <= s_Done;
						oC <= regA;
					end
				end
					
			//----------------------- s_Done
			s_Done:
				begin
					oValid <= 1;	
					oReady <= 1;
					state <= s_Idle;
				end
			endcase
		end			
	end
	
endmodule