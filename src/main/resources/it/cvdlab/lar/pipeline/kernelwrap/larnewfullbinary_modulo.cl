// #define VECTOR_INDEX(A) (A) + get_group_id(0) * INPUTVECTORSIZE
// #define RESULT_INDEX(A)	(A) + get_group_id(0) * ROWSIZE

// rowSize => #define ROWSIZE
// oldVectorSize => #define OLDVECTORSIZE
// inputVectorSize => #define INPUTVECTORSIZE
// totalVectors => #define TOTALVECTORSSIZE

#define INTEGER_BIT_SIZE 32

__kernel void many_vec_mul_local_bitwise_binary(
          __global const unsigned int * row_indices,
          __global const unsigned int * column_indices,
          __global const unsigned int * vectorBig,
          __global unsigned char * resultBig,
          const uint rowDivisible, const uint vectorDivisible,
          __local unsigned int * localVector)
{
	// Offset di moltiplicazione per riga
	unsigned int work_per_item = max((uint) (ROWSIZE / get_local_size(0)) + rowDivisible, (uint) 1);
	unsigned int row_start = get_local_id(0) * work_per_item;
	unsigned int row_stop  = min( (uint) ((get_local_id(0) + 1) * work_per_item), (uint) ROWSIZE);
  
  	// Shortcut al vettore locale del gruppo
	__global const unsigned int * vector = vectorBig + get_group_id(0) * INPUTVECTORSIZE;
	__global unsigned char * result = resultBig + get_group_id(0) * ROWSIZE;
  
	// Variabili riutilizzabili
	unsigned int row;
	unsigned int row_end;
	unsigned int dot_prod;
	unsigned int bitToCheck;
	unsigned int currCol;
  
	// ==== Local copy ====
	unsigned int copy_per_item = max((uint) (INPUTVECTORSIZE / get_local_size(0)) + vectorDivisible, (uint) 1);
	unsigned int el_start = get_local_id(0) * copy_per_item;
	unsigned int el_stop  = min( (uint) ((get_local_id(0) + 1) * copy_per_item), (uint) INPUTVECTORSIZE);
	
	// printf("lwid: %d, [%d, %d, %d, %d]\n", get_local_id(0), copy_per_item, el_start, el_stop);
	 
	for (row = el_start; row < el_stop; ++row) {  
  		localVector[ row ] = vector[ row ];
  	}
  
	// Sincronizza  
	barrier(CLK_LOCAL_MEM_FENCE);
  	
	// Inizia computazione
	for (unsigned int row = row_start; row < row_stop; ++row) {
  		dot_prod = 0;
		row_end = row_indices[row+1];
   
		for (unsigned int i = row_indices[row]; i < row_end; ++i) {
			currCol = column_indices[i];
			// should be: currCol >> log2(INTEGER_BIT_SIZE)
			bitToCheck = currCol - INTEGER_BIT_SIZE*(currCol/INTEGER_BIT_SIZE);
			dot_prod += ((localVector[ currCol / INTEGER_BIT_SIZE ] & (1 << bitToCheck)) >> bitToCheck);
		}
		result[ row ] = (unsigned char)(dot_prod & 1); // modulo 2
	}
}

__kernel void many_vec_mul_bitwise_binary(
          __global const unsigned int * row_indices,
          __global const unsigned int * column_indices,
          __global const unsigned int * vectorBig,
          __global unsigned char * resultBig,
          const uint rowDivisible)
{
	// Offset di moltiplicazione per riga
	// printf("Div intera %d\n", (uint) (ROWSIZE / get_local_size(0)) );
	unsigned int work_per_item = max((uint) (ROWSIZE / get_local_size(0)) + rowDivisible, (uint) 1);
	unsigned int row_start = get_local_id(0) * work_per_item;
	unsigned int row_stop  = min( (uint) ((get_local_id(0) + 1) * work_per_item), (uint) ROWSIZE);
  
  	// Shortcut al vettore locale del gruppo
	__global const unsigned int * vector = vectorBig + get_group_id(0) * INPUTVECTORSIZE;
	__global unsigned char * result = resultBig + get_group_id(0) * ROWSIZE;
  
	// Variabili riutilizzabili
	unsigned int row;
	unsigned int row_end;
	unsigned int dot_prod;
	unsigned int bitToCheck;
	unsigned int currCol;
  	
	// Inizia computazione
	for (unsigned int row = row_start; row < row_stop; ++row) {
  		dot_prod = 0;
		row_end = row_indices[row+1];
   
		for (unsigned int i = row_indices[row]; i < row_end; ++i) {
			currCol = column_indices[i];
			// should be: currCol >> log2(INTEGER_BIT_SIZE)
			bitToCheck = currCol - INTEGER_BIT_SIZE*(currCol/INTEGER_BIT_SIZE);
			dot_prod += ((vector[ currCol / INTEGER_BIT_SIZE ] & (1 << bitToCheck)) >> bitToCheck);
		}
		result[ row ] = (unsigned char)(dot_prod & 1); // modulo 2
	}
}