// C = A B
// spezzo la matrice A in x gruppi di y righe
// ogni core si fa N righe nel gruppo

__kernel void m_mul_m(
          __global const unsigned int * row_indices_a,
          __global const unsigned int * column_indices_a,
		  __global const unsigned int * data_a,
          __global const unsigned int * row_indices_b,
          __global const unsigned int * column_indices_b,
		  __global const unsigned int * data_b,
		  // Result
		  __global const unsigned int * rowPtr,
		  __global unsigned int * colData,
          __global unsigned int * data,
		  // Workslice
          const uint work_per_item)
{
	// Offset di moltiplicazione per riga ( tutta la computazione dei moduli sull'host)
	unsigned int row_start = get_local_id(0) * get_group_id(0) * work_per_item;
	unsigned int row_stop  = min( (uint) ((get_local_id(0) + 1) * get_group_id(0) * work_per_item), (uint) ROWS_A);
  
	// Variabili riutilizzabili
	unsigned int row_a;
	unsigned int row_b;
	
	unsigned int col_ptr_a;
	unsigned int col_ptr_end_a;
	unsigned int col_ptr_b;
	unsigned int col_ptr_end_b;
  	unsigned int col_val_a;
  	unsigned int col_val_b;
  	
	unsigned int col_val_res_start;
	unsigned int col_val_res_end;
	
	unsigned int curr_res = 0;
	
	// Inizia computazione
	for (row_a = row_start; row_a < row_stop; ++row_a) {
  		nnz_row = 0;
		col_ptr_end_a = row_indices_a[row_a + 1];
		// Result
		col_val_res_start = rowPtr[row_a];
		col_val_res_end = rowPtr[row_a + 1];
		
		// copia in loacal cache tutta la riga cosi' da evitare di rileggere dalla mem.globale
   
   		// questo for deve essere scalato, ogni wi parte da una riga diversa e gira
   		// evita serializzazione d'accesso alla memoria
   		// qualcosa tipo row_b = get_global_id; row_b < ROWS_B + get_global_id
   		// (rowb + x) % ROWS_B, se ROWS_B è potenza di 2 è pure meglio
		for(unsigned int row_b = 0; (row_b < ROWS_B) && (col_val_res_start < col_val_res_end); ++row_b) {
			col_ptr_end_b = row_indices_b[row_b + 1];
			
			// While variables
			col_ptr_a = row_indices_a[row_a];
			col_ptr_b = row_indices_b[row_b];
			
			while ((col_ptr_a < col_ptr_end_a) && (col_ptr_b < col_ptr_end_b)) {
			
				col_val_a = column_indices_a[col_ptr_a];
				col_val_b = column_indices_b[col_ptr_b];
			
				if (col_val_a == col_val_b) {
					curr_res = curr_res + data_a[col_ptr_a] * data_b[col_ptr_b];
				} else if ( col_val_a < col_val_b) {
					col_ptr_a++;
				} else {
					col_ptr_b++;
				}
			}
			
			colData[ col_val_res_start ] = row_b;
			data[ col_val_res_start ] = curr_res;
			col_val_res_start++;
		}
	}
}