// #define VECTOR_INDEX(A) (A) + get_group_id(0) * INPUTVECTORSIZE
// #define RESULT_INDEX(A)	(A) + get_group_id(0) * ROWSIZE

// rowSize => #define ROWSIZE
// inputVectorSize => #define INPUTVECTORSIZE
// totalVectors => #define TOTALVECTORSSIZE


__kernel void many_vec_mul_local_binary(
          __global const unsigned int * row_indices,
          __global const unsigned int * column_indices,
          __global float * vectorBig,
          __global float * resultBig,
          __local float * localVector)
{
	// Offset di moltiplicazione per riga
	unsigned int work_per_item = max((uint) (ROWSIZE / get_local_size(0)), (uint) 1);
	unsigned int row_start = get_local_id(0) * work_per_item;
	unsigned int row_stop  = min( (uint) ((get_local_id(0) + 1) * work_per_item), (uint) ROWSIZE);
  
  	// Shortcut al vettore locale del gruppo
	__global const float * vector = vectorBig + get_group_id(0) * INPUTVECTORSIZE;
	__global float * result = resultBig + get_group_id(0) * ROWSIZE;
  
	// Variabili riutilizzabili
	unsigned int row;
	unsigned int row_end;
	float dot_prod;
  
	// ==== Local copy ====
	unsigned int copy_per_item = max((uint) (INPUTVECTORSIZE / get_local_size(0)), (uint) 1);
	unsigned int el_start = get_local_id(0) * copy_per_item;
	unsigned int el_stop  = min( (uint) ((get_local_id(0) + 1) * copy_per_item), (uint) INPUTVECTORSIZE);
	 
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
			// dot_prod += elements[i] * localVector[ column_indices[i] ];
			// float multiplication and addition, this is more performing when supported
			dot_prod = fma(1.0, localVector[ column_indices[i] ], dot_prod);
		}
		result[ row ] = dot_prod;
	}
}


/*
inline float computeMultiplicationVec(__global const unsigned int * row_indices, 
								__global const unsigned int * column_indices,
								__global const float * elements,
								__global const float * vector,
								unsigned int row,
								unsigned int row_end) {
								
	float dot_prod = 0.0f;
	
	// vec4: a freddo + lento, a caldo paragonabile
	float4 v_vector;
	float4 v_data;
	int4 v_hasdata;
	for(unsigned int i = row_indices[row]; i < row_end; i+=4 ) {											
		v_data.x = elements[i];
		v_data.y = elements[i+1];
		v_data.z = elements[i+2];
		v_data.w = elements[i+3];
		v_hasdata = ((int4)(i,i+1,i+2,i+3) <= ((int)row_end - 1));
		v_vector.x = v_hasdata.x ? vector[  column_indices[i]   ] : 0;
		v_vector.y = v_hasdata.y ? vector[  column_indices[i+1] ] : 0;	
		v_vector.z = v_hasdata.z ? vector[  column_indices[i+2] ] : 0;
		v_vector.w = v_hasdata.w ? vector[  column_indices[i+3] ] : 0;
		dot_prod += dot(v_data,v_vector);
	}

	return dot_prod;
}

*/