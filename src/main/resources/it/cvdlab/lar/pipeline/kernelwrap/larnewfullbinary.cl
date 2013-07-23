// #define VECTOR_INDEX(A) (A) + get_group_id(0) * INPUTVECTORSIZE
// #define RESULT_INDEX(A)	(A) + get_group_id(0) * ROWSIZE

// rowSize => #define ROWSIZE
// oldVectorSize => #define OLDVECTORSIZE
// inputVectorSize => #define INPUTVECTORSIZE
// totalVectors => #define TOTALVECTORSSIZE

#define INTEGER_BIT_SIZE 32

__kernel void many_vec_mul_local_bitwise(
          __global const unsigned int * row_indices,
          __global const unsigned int * column_indices,
          __global const float * elements,
          __global int * vectorBig,
          __global float * resultBig,
          __local int * localVector)
{
	// Offset di moltiplicazione per riga
	unsigned int work_per_item = max((uint) (ROWSIZE / get_local_size(0)), (uint) 1);
	unsigned int row_start = get_local_id(0) * work_per_item;
	unsigned int row_stop  = min( (uint) ((get_local_id(0) + 1) * work_per_item), (uint) ROWSIZE);
  
  	// Shortcut al vettore locale del gruppo
	__global const int * vector = vectorBig + get_group_id(0) * INPUTVECTORSIZE;
	__global float * result = resultBig + get_group_id(0) * ROWSIZE;
  
	// Variabili riutilizzabili
	unsigned int row;
	unsigned int row_end;
	float dot_prod;
	unsigned int bitToCheck;
	unsigned int currCol;
  
	// ==== Local copy ====
	unsigned int copy_per_item = max((uint) (INPUTVECTORSIZE / get_local_size(0)), (uint) 1);
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
  		dot_prod = 0.0f;
		row_end = row_indices[row+1];
   
		for (unsigned int i = row_indices[row]; i < row_end; ++i) {
			currCol = column_indices[i];
			bitToCheck = currCol - INTEGER_BIT_SIZE*(currCol/OLDVECTORSIZE);
			// printf("%d , %d => %d , %d [%d]\n", i, column_indices[i], vector[ column_indices[i] / OLDVECTORSIZE ], 1 << bitToCheck, bitToCheck);
			dot_prod += elements[i] * ((vector[ currCol / OLDVECTORSIZE ] & (1 << bitToCheck)) >> bitToCheck);
			// dot_prod = fma(elements[i], ((vector[ column_indices[i] / OLDVECTORSIZE ] & (1 << bitToCheck)) >> bitToCheck), dot_prod );
		}
		result[ row ] = dot_prod;
		
		// Oppure (usa a botte di 4)
		// result[row] = computeMultiplicationVec(row_indices, column_indices, elements, localVector, row, row_end);
	}
}

__kernel void many_vec_mul_local_bitwise_binary(
          __global const unsigned int * row_indices,
          __global const unsigned int * column_indices,
          __global int * vectorBig,
          __global int * resultBig,
          __local int * localVector)
{
	// Offset di moltiplicazione per riga
	unsigned int work_per_item = max((uint) (ROWSIZE / get_local_size(0)), (uint) 1);
	unsigned int row_start = get_local_id(0) * work_per_item;
	unsigned int row_stop  = min( (uint) ((get_local_id(0) + 1) * work_per_item), (uint) ROWSIZE);
  
  	// Shortcut al vettore locale del gruppo
	__global const int * vector = vectorBig + get_group_id(0) * INPUTVECTORSIZE;
	__global int * result = resultBig + get_group_id(0) * ROWSIZE;
  
	// Variabili riutilizzabili
	unsigned int row;
	unsigned int row_end;
	unsigned int dot_prod;
	unsigned int bitToCheck;
	unsigned int currCol;
  
	// ==== Local copy ====
	unsigned int copy_per_item = max((uint) (INPUTVECTORSIZE / get_local_size(0)), (uint) 1);
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
			bitToCheck = currCol - INTEGER_BIT_SIZE*(currCol/OLDVECTORSIZE);
			dot_prod += 1 * ((localVector[ currCol / OLDVECTORSIZE ] & (1 << bitToCheck)) >> bitToCheck);
		}
		result[ row ] = dot_prod;
	}
}