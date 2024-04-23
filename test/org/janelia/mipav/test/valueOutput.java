package org.janelia.mipav.test;


import java.io.FileWriter;
import java.io.IOException;

public class valueOutput {
    private FileWriter fileWriter;

    public valueOutput(String fileName) {
        try {
			this.fileWriter = new FileWriter(fileName);
	        // Writing the header
	        this.fileWriter.append("p0.X,p0.Y,p0.Z,value\n");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

    }

    public void writeData(float x, float y, float z, float value) {
        // Writing data in the CSV format
        try {
			this.fileWriter.append(String.format("%.1f,%.1f,%.1f,%.1f\n", x, y, z, value));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

    public void close() {
        // Closing the FileWriter
        try {
			this.fileWriter.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
}



