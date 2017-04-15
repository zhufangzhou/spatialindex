
import java.io.*;
import java.util.*;

import spatialindex.spatialindex.*;
import spatialindex.storagemanager.*;
import spatialindex.rtree.*;

public class AccumulateRTree {
    public static void main(String[] args) {
        new AccumulateRTree(args);
    }

    AccumulateRTree(String[] args) {
        try {
            if (args.length != 1) {
                System.err.println("Usage: AccumulateRTree input_file");
                System.exit(-1);
            }

            // Create Reader to read input file
            LineNumberReader lr = null;
            try {
                lr = new LineNumberReader(new FileReader(args[0]));
            } catch (FileNotFoundException e) {
                System.err.println("Cannot open data file " + args[0] + ".");
                System.exit(-1);
            }

            PropertySet ps = new PropertySet();
            ps.setProperty("Overwrite", new Boolean(true));
            ps.setProperty("FileName", "aRTree");
            ps.setProperty("PageSize", new Integer(4096));

            IStorageManager diskfile = new DiskStorageManager(ps);
            IBuffer file = new RandomEvictionsBuffer(diskfile, 10, false);

            PropertySet ps2 = new PropertySet();
            ps2.setProperty("FillFactor", new Double(0.7));
            ps2.setProperty("IndexCapacity", new Integer(3));
            ps2.setProperty("LeafCapacity", new Integer(3));
            ps2.setProperty("Dimension", new Integer(3));

            ISpatialIndex tree = new RTree(ps2, file);

            // Read data line by line and pack them into bulk
            int count = 0;
            String line = lr.readLine(), trajIds;
            double x, y, z;
            double [] pt = new double[3];

            // Declare Bulk Object
            // IShape[] rs = new Region[2];
            // byte[][] datas = new byte[2][];
            // int[] ids = new int[2];
            List<IShape> rs = new ArrayList<IShape>();
            List<byte[]> datas = new ArrayList<byte[]>();
            List<Integer> ids = new ArrayList<Integer>();
            byte[] tmp = new byte[1];

            while (line != null) {

                StringTokenizer st = new StringTokenizer(line);
                x = new Double(st.nextToken()).doubleValue();
                y = new Double(st.nextToken()).doubleValue();
                z = new Double(st.nextToken()).doubleValue();
                trajIds = st.nextToken();
                // System.out.println(x + "," + y + "," + z + " | " + trajIds);

                pt[0] = x; pt[1] = y; pt[2] = z;
                Region r = new Point(pt).getMBR();

                // tree.insertData(trajIds.getBytes(), r, count);
                // rs[count] = r;
                // datas[count] = trajIds.getBytes();
                // ids[count] = count;
                rs.add(r);
                datas.add(trajIds.getBytes());
                ids.add(count);

                count++;
                line = lr.readLine();
            }
            tree.bulkLoading(datas, rs, ids);
            System.out.println("Total line number: " + count);
            // System.out.println(tree.toString());
            tree.flush();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}