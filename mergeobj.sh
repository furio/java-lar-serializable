#!/bin/sh
echo "Merging vertices and faces"
cat outputVtx.obj > output.obj
cat outputFaces.obj >> output.obj
echo "Done"
