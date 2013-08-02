package it.cvdlab.lar.pipeline.helpers.cuda;

public enum CudaCoordinates {
    X (0),
    Y (1),
    Z (2);

    private final int index;   

    CudaCoordinates(int index) {
        this.index = index;
    }

    public int index() { 
        return index; 
    }
    
    public static int length() { 
        return CudaCoordinates.values().length; 
    }    
}
