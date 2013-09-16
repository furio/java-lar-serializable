from lar import *
from scipy import *
import json
import scipy
import numpy as np
import time as tm
import gc
import struct

# ------------------------------------------------------------
# Logging & Timer 
# ------------------------------------------------------------

logging_level = 2; 

# 0 = no_logging
# 1 = few details
# 2 = many details
# 3 = many many details

def log(n, l):

    if __name__=="__main__" and n <= logging_level:
        for s in l:
            print "Log:", s;

timer = 1;

timer_last =  tm.time()

def timer_start(s):

    global timer_last;
    if __name__=="__main__" and timer == 1:   
        print "Timer start:", s;
    timer_last = tm.time();

def timer_stop():

    global timer_last;
    if __name__=="__main__" and timer == 1:   
        print "Timer stop :", tm.time() - timer_last;

# ------------------------------------------------------------
# Configuration parameters
# ------------------------------------------------------------

nx = ny = nz = 64
log(1, ["nx, ny, nz = " + str(nx) + "," + str(ny) + "," + str(nz)]);

chunksize = nx * ny + nx * nz + ny * nz + 3 * nx * ny * nz
log(1, ["chunksize = " + str(chunksize)]);
# chunksize = 798720 (nx=ny=nz=64)

MAX_CHUNKS = 64 #1024
log(1, ["MAX_CHUNKS = " + str(MAX_CHUNKS)]);

# ------------------------------------------------------------

def ind(x,y,z): return x + (nx+1) * (y + (ny+1) * (z))

def invertIndex(nx,ny,nz):
	nx,ny,nz = nx+1,ny+1,nz+1
	def invertIndex0(offset):
		a0, b0 = offset / nx, offset % nx
		a1, b1 = a0 / ny, a0 % ny
		a2, b2 = a1 / nz, a1 % nz
		return b0,b1,b2
	return invertIndex0

# Construction of vertex coordinates (nx * ny * nz)
# ------------------------------------------------------------

timer_start("V");

V = [[x,y,z] for z in range(nz+1) for y in range(ny+1) for x in range(nx+1) ]

timer_stop();
log(3, ["V = " + str(V)]);

# Construction of FV relation (nx * ny * nz)
# ------------------------------------------------------------

FV = []

timer_start("v2coords");

v2coords = invertIndex(nx,ny,nz)

timer_stop();

timer_start("h");

for h in range(len(V)):
	x,y,z = v2coords(h)
	if (x < nx) and (y < ny): FV.append([h,ind(x+1,y,z),ind(x,y+1,z),ind(x+1,y+1,z)])
	if (x < nx) and (z < nz): FV.append([h,ind(x+1,y,z),ind(x,y,z+1),ind(x+1,y,z+1)])
	if (y < ny) and (z < nz): FV.append([h,ind(x,y+1,z),ind(x,y,z+1),ind(x,y+1,z+1)])

timer_stop();
log(3, ["FV = " + str(FV)]);

# ------------------------------------------------------------

out = []
file = open("output.bin", "rb")

fileVertex = open("outputVtx.obj", "w")
fileFaces = open("outputFaces.obj", "w")

vertex_count = 1
old_vertex_count = vertex_count
count = 0

for i in range(MAX_CHUNKS):

	count += 1

	zStart = struct.unpack('>I', file.read(4))[0]
	xStart = struct.unpack('>I', file.read(4))[0]
	yStart = struct.unpack('>I', file.read(4))[0]

	log(1, ["zStart, xStart, yStart = " + str(zStart) + "," + str(xStart) + "," + str(yStart)]);
#	zStart, xStart, yStart = LISTA_OFFSET[i].astype(float64)

	LISTA_VETTORI2 = np.zeros(chunksize,dtype=int32);

	temp = file.read(chunksize);

	timer_start("LISTA_VETTORI2 " + str(i));
	i = 0
	while (i < chunksize):
		if (temp[i] == '\x01'):
			LISTA_VETTORI2[i] = 1;
		i = i + 1;
	timer_stop();
	
	timer_start("objectBoundaryChain ");
	l = len(LISTA_VETTORI2)
	objectBoundaryChain = scipy.sparse.csr_matrix(LISTA_VETTORI2.reshape((l,1)))
	timer_stop();

	timer_start("csrChainToCellList " + str(i));
	b2cells = csrChainToCellList(objectBoundaryChain)
	timer_stop();

	timer_start("MKPOLS " + str(i));
	
	for f in b2cells:
		old_vertex_count = vertex_count
		
		for vtx in FV[f]:
			fileVertex.write("v ")
			fileVertex.write(str(V[vtx][0] + zStart))
			fileVertex.write(" ")
			fileVertex.write(str(V[vtx][1] + xStart))
			fileVertex.write(" ")
			fileVertex.write(str(V[vtx][2] + yStart))
			fileVertex.write("\n")
			vertex_count = vertex_count + 1
			
		fileFaces.write("f ")
		fileFaces.write(str(old_vertex_count + 0))
		fileFaces.write(" ")
		fileFaces.write(str(old_vertex_count + 1))
		fileFaces.write(" ")
		fileFaces.write(str(old_vertex_count + 3))
		fileFaces.write("\n")

		fileFaces.write("f ")
		fileFaces.write(str(old_vertex_count + 0))
		fileFaces.write(" ")
		fileFaces.write(str(old_vertex_count + 3))
		fileFaces.write(" ")
		fileFaces.write(str(old_vertex_count + 2))
		fileFaces.write("\n")		

	timer_stop();

	if count == MAX_CHUNKS:
		timer_start("VIEW(STRUCT(out))");
		timer_stop();

file.close()
fileVertex.close()
fileFaces.close()

