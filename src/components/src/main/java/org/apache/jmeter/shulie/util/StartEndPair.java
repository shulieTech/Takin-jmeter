package org.apache.jmeter.shulie.util;

/**
 * @author 李鹏
 */
@SuppressWarnings("unused")
public class StartEndPair {

    private String partition;
    private long start;
    private long end;

    public String getPartition() {return partition;}

    public void setPartition(String partition) {this.partition = partition;}

    public long getStart() {return start;}

    public long getEnd() {return end;}

    public void setStart(long start) {this.start = start;}

    public void setEnd(long end) {this.end = end;}

    @Override
    public String toString() {return "star=" + start + ";end=" + end + ";partition=" + partition;}

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int)(end ^ (end >>> 32));
        result = prime * result + (int)(start ^ (start >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof StartEndPair) {
            StartEndPair oldObj = (StartEndPair)obj;
            return oldObj.getEnd() == this.getEnd()
                && oldObj.getStart() == this.getStart()
                && oldObj.getPartition().equals(this.getPartition());
        } else {
            return super.equals(obj);
        }
    }
}
