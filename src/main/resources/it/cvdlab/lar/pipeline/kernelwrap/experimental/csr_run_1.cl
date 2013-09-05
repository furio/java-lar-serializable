// C = A B
// spezzo la matrice A in x gruppi di y righe
// ogni core si fa N righe nel gruppo


__kernel void m_mul_m_count(
          __global const unsigned int * row_indices_a,
          __global const unsigned int * column_indices_a,
          __global const unsigned int * row_indices_b,
          __global const unsigned int * column_indices_b,          
          __global unsigned int * resultBig,
          const uint work_per_item)
{
	// Offset di moltiplicazione per riga ( tutta la computazione dei moduli sull'host)
	unsigned int row_start = get_global_id(0) * work_per_item;
	unsigned int row_stop  = min( (uint) ((get_global_id(0) + 1) * work_per_item), (uint) ROWS_A);
  
	// Variabili riutilizzabili
	unsigned int row_a;
	unsigned int row_b;
	unsigned int nnz_row;
	
	unsigned int col_ptr_a;
	unsigned int col_ptr_end_a;
	unsigned int col_ptr_b;
	unsigned int col_ptr_end_b;
  	unsigned int col_val_a;
  	unsigned int col_val_b;
	
	unsigned char foundNNZ;
  	
	// Inizia computazione
	for (row_a = row_start; row_a < row_stop; ++row_a) {
  		nnz_row = 0;
		col_ptr_end_a = row_indices_a[row_a + 1];
		
		// copia in loacal cache tutta la riga cosi' da evitare di rileggere dalla mem.globale
   
   		// questo for deve essere scalato, ogni wi parte da una riga diversa e gira
   		// evita serializzazione d'accesso alla memoria
   		// qualcosa tipo row_b = get_global_id; row_b < ROWS_B + get_global_id
   		// (rowb + x) % ROWS_B, se ROWS_B è potenza di 2 è pure meglio
		for(unsigned int row_b = 0; row_b < ROWS_B; ++row_b) {
			col_ptr_end_b = row_indices_b[row_b + 1];
			
			// While variables
			col_ptr_a = row_indices_a[row_a];
			col_ptr_b = row_indices_b[row_b];
			foundNNZ = 0;
			
			while ((foundNNZ == 0) && (col_ptr_a < col_ptr_end_a) && (col_ptr_b < col_ptr_end_b)) {
			
				col_val_a = column_indices_a[col_ptr_a];
				col_val_b = column_indices_b[col_ptr_b];
			
				if (col_val_a == col_val_b) {
					nnz_row = nnz_row + 1;
					foundNNZ = 1;
				} else if ( col_val_a < col_val_b) {
					col_ptr_a++;
				} else {
					col_ptr_b++;
				}
			}
		}
		
		resultBig[ row_a ] = nnz_row;
	}
	
	barrier(CLK_GLOBAL_MEM_FENCE);
	if ((row_stop == ROWS_A) && (row_start < row_stop)) {
		resultBig[ ROWS_A ] = resultBig[ ROWS_A - 1 ];
	}
}