package spatialindex.transition;

import java.io.*;
import java.util.*;
import java.lang.reflect.Method;

import spatialindex.spatialindex.*;
import spatialindex.storagemanager.*;
import spatialindex.rtree.*;

public class TransitionMatrix {

    int m_indexCapacity = 3;
    int m_leafCapacity = 3;
    int m_dimension = 2;

    public static <T> byte[] objectToByteArray(T obj) {
        byte[] b = null;
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(obj);
            b = bos.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return b;
    }

    public static <T> T byteArrayToObject(byte[] b, Class<T> c) {
        T obj = null;
        try {
            ByteArrayInputStream bin = new ByteArrayInputStream(b);
            ObjectInputStream ois = new ObjectInputStream(bin);
            obj = (T) ois.readObject();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return obj;
    }

    public TransitionMatrix(String dataPath, String treeName, int indexCapacity, int leafCapacity, int dimension) {
        // Assign attributes
        m_indexCapacity = indexCapacity;
        m_leafCapacity = leafCapacity;
        m_dimension = dimension;

        // Try to open file reader
        LineNumberReader lr = null;
        try {
            lr = new LineNumberReader(new FileReader(dataPath));
        } catch (FileNotFoundException e) {
            System.err.println("Cannot open data file " + dataPath + ".");
            System.exit(-1);
        }

        try {
            PropertySet ps = new PropertySet();
            ps.setProperty("Overwrite", new Boolean(true));
            ps.setProperty("FileName", "aRTree");
            ps.setProperty("PageSize", new Integer(4096));

            IStorageManager diskfile = new DiskStorageManager(ps);
            IBuffer file = new RandomEvictionsBuffer(diskfile, 10, false);

            PropertySet ps2 = new PropertySet();
            ps2.setProperty("FillFactor", new Double(0.7));
            ps2.setProperty("IndexCapacity", new Integer(m_indexCapacity));
            ps2.setProperty("LeafCapacity", new Integer(m_leafCapacity));
            ps2.setProperty("Dimension", new Integer(m_dimension));

            ISpatialIndex tree = new RTree(ps2, file);

            String line = lr.readLine();
            int id = 0;
            double x, y, z;
            double[] pt = new double[3];
            List<IShape> rList = new ArrayList<IShape>();
            List<byte[]> dataList = new ArrayList<byte[]>();
            List<Integer> idList = new ArrayList<Integer>();
            while (line != null) {
                StringTokenizer st = new StringTokenizer(line, " ");
                x = new Double(st.nextToken()).doubleValue();
                y = new Double(st.nextToken()).doubleValue();
                z = new Double(st.nextToken()).doubleValue();
                // The trajectory id set are separated by comma
                StringTokenizer trajSt = new StringTokenizer(st.nextToken(), ",");
                Set<Integer> tidSet = new HashSet<Integer>();
                while (trajSt.hasMoreTokens()) {
                    Integer tid = new Integer(trajSt.nextToken());
                    tidSet.add(tid);
                }
                // cast HashSet to byte array
                byte[] data = objectToByteArray(tidSet);

                pt[0] = x;
                pt[1] = y;
                pt[2] = z;
                Region r = new Point(pt).getMBR();

                rList.add(r);
                dataList.add(data);
                idList.add(id);

                id++;
                line = lr.readLine();
            }
            tree.bulkLoading(dataList, rList, idList, TransitionMatrix.class.getMethod("accumulateData",
                    new Class<?>[] { new ArrayList<byte[]>().getClass() }));
            tree.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static int accumulateData(ArrayList<byte[]> leafData) {
        Set<Integer> trajIdSet = new HashSet<Integer>();
        for (int cIndex = 0; cIndex < leafData.size(); cIndex++) {
            HashSet<Integer> tmpSet = TransitionMatrix.byteArrayToObject(leafData.get(cIndex), new HashSet<Integer>().getClass());
            trajIdSet.addAll(tmpSet);
        }
        return trajIdSet.size();
    }
}