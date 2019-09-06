/*
 * Copyright (c) 2013 idealo internet GmbH -- all rights reserved.
 */
package de.idealo.mongodb.slowops.grapher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.HashSet;

/**
 * 
 * 
 * @author kay.agahd
 * @since 14.03.2013
 * @version $Id: $
 * @copyright idealo internet GmbH
 */
public class AggregatedProfiling {

    private static final Logger LOG = LoggerFactory.getLogger(Grapher.class);


    private AggregatedProfilingId _id;
    private int count;
    private long millis;
    private double avgMs;
    private double minMs;
    private double maxMs;
    private double stdDevMs;
    private double nRet;
    private double avgRet;
    private double minRet;
    private double maxRet;
    private double stdDevRet;
    private Date firstts;

    private long keys;
    private long docs;
    private long del;
    private long ins;
    private long mod;
    private HashSet<Boolean> sortstages;


    private AggregatedProfiling(){
        
    }

    /**
     * @return the _id
     */
    public AggregatedProfilingId getId() {
        return _id;
    }

    /**
     * @return the count
     */
    public int getCount() {
        return count;
    }

    /**
     * @return the millis
     */
    public long getMillis() {
        return millis;
    }

    /**
     * @return the avgMs
     */
    public double getAvgMs() {
        return avgMs;
    }

    /**
     * @return the minMs
     */
    public double getMinMs() {
        return minMs;
    }

    /**
     * @return the maxMs
     */
    public double getMaxMs() {
        return maxMs;
    }

    /**
     * @return the stdDevMs
     */
    public double getStdDevMs() {
        return stdDevMs;
    }

    /**
     * @return the nRet
     */
    public double getNRet() {
        return nRet;
    }

    /**
     * @return the avgRet
     */
    public double getAvgRet() {
        return avgRet;
    }

    /**
     * @return the minRet
     */
    public double getMinRet() {
        return minRet;
    }

    /**
     * @return the maxRet
     */
    public double getMaxRet() {
        return maxRet;
    }

    /**
     * @return the stdDevRet
     */
    public double getStdDevRet() {
        return stdDevRet;
    }

    /**
     * @return the firstts
     */
    public Date getFirstts() {
        return firstts;
    }


    public long getKeys() { return keys; }

    public long getDocs() { return docs; }

    public boolean hasSortStage() { return sortstages.contains(Boolean.TRUE); }

    public long getDel() { return del; }

    public long getIns() { return ins; }

    public long getMod() { return mod; }

    public long getDocsWrittenCount() { return del+ins+mod; }

    public HashSet<Boolean> getSortstages() { return sortstages; }


    /**
     * @param entry
     */
    public void combine(AggregatedProfiling entry) {
        if(entry != null) {
            

            if(entry.getMinMs() < minMs) minMs = entry.getMinMs();
            if(entry.getMaxMs() > maxMs) maxMs = entry.getMaxMs();
            stdDevMs = combineStdDev(new Triplet(millis, stdDevMs, count), new Triplet(entry.getMillis(), entry.getStdDevMs(), entry.getCount()));
            if(entry.getMinRet() < minRet) minRet = entry.getMinRet();
            if(entry.getMaxRet() > maxRet) maxRet = entry.getMaxRet();
            stdDevRet = combineStdDev(new Triplet(nRet, stdDevRet, count), new Triplet(entry.getNRet(), entry.getStdDevRet(), entry.getCount()));
            if(entry.getFirstts().before(firstts)) firstts = entry.getFirstts();
            keys += entry.getKeys();
            docs += entry.getDocs();
            del += entry.getDel();
            ins += entry.getIns();
            mod += entry.getMod();
            sortstages.addAll(entry.getSortstages());
            nRet += entry.getNRet();//last line because it's used to calculate stdDev
            millis += entry.getMillis();//last line because it's used to calculate stdDev
            count += entry.getCount();//last line because it's used to calculate stdDev
        }
    }

    private double combineStdDev(Triplet t1, Triplet t2){
        double bothAvg = (t1.value+t2.value)/(t1.count+t2.count);
        return Math.sqrt(
                (
                   (Math.pow(t1.value/t1.count + t1.stdDev - bothAvg, 2.0)*(t1.count/2.0)) +
                   (Math.pow(t1.value/t1.count - t1.stdDev - bothAvg, 2.0)*(t1.count/2.0)) +
                   (Math.pow(t2.value/t2.count + t2.stdDev - bothAvg, 2.0)*(t2.count/2.0)) +
                   (Math.pow(t2.value/t2.count - t2.stdDev - bothAvg, 2.0)*(t2.count/2.0))
                )
                / (t1.count + t2.count)
        );
    }

    @Override
    protected AggregatedProfiling clone() {
        final AggregatedProfiling result = new AggregatedProfiling();
        result._id = getId();
        result.count = getCount();
        result.millis = getMillis();
        result.avgMs = getAvgMs();
        result.minMs = getMinMs();
        result.maxMs = getMaxMs();
        result.stdDevMs = getStdDevMs();
        result.nRet = getNRet();
        result.avgRet = getAvgRet();
        result.minRet = getMinRet();
        result.maxRet = getMaxRet();
        result.stdDevRet = getStdDevRet();
        result.firstts = (Date)getFirstts().clone();
        result.keys = getKeys();
        result.docs = getDocs();
        result.del = getDel();
        result.ins = getIns();
        result.mod = getMod();
        result.sortstages = getSortstages();

        return result;
    }

    private class Triplet{
        final double value, stdDev, count;
        public Triplet(double value, double stdDev, double count){
            this.value = value;
            this.stdDev = stdDev;
            this.count = count;
        }
    }

    public static void main(String[] args) {
        int [][] samples = {
                {2,4,4,4,5,5,7,9,10,10},//first sample, avg=5, sigma=2
                {20,40,40,40,50,50,70,90,30,30},//second sample, , avg=50, sigma=20
                {2,4,4,4,5,5,7,9,20,40,40,40,50,50,70,90,10,30,30,10}//both samples merged together to one sample, , avg=27.5, sigma=26.6
        };


        int sum[] = {0,0,0};
        int avg[] = {0,0,0};
        for (int s = 0; s < sum.length; s++) {
            for (int i = 0; i < samples[s].length; i++) {
                sum[s] += samples[s][i];
            }
            System.out.println("sum["+s+"]=" + sum[s]);
            avg[s] += (double)sum[s]/(double)samples[s].length;
            System.out.println("avg["+s+"]=" + avg[s]);
        }

        int sumSig[] = {0,0,0};
        double sigma[] = {0,0,0};
        for (int s = 0; s < sumSig.length; s++) {
            for (int i = 0; i < samples[s].length; i++) {
                sumSig[s] += Math.pow(samples[s][i]-avg[s], 2.0);
            }
            System.out.println("sumSig["+s+"]=" + sumSig[s]);

            sigma[s] += Math.sqrt((double)sumSig[s]/(double)samples[s].length);
            System.out.println("Sigma["+s+"]=" + sigma[s]);
        }

        //double sigmaBoth = Math.sqrt( ((Math.pow(avg[0]+sigma[0]-avg[2], 2.0) )*samples[0].length + (Math.pow(avg[1]+sigma[1]-avg[2], 2.0) )*samples[1].length)/samples[2].length );
        double sample1Odd = samples[0].length%2==0?0:(Math.pow(avg[0]-avg[2], 2.0) );
        double sample2Odd = samples[1].length%2==0?0:(Math.pow(avg[1]-avg[2], 2.0) );
        double sample1Len = samples[0].length%2==0?(samples[0].length/2):((samples[0].length-1)/2);
        double sample2Len = samples[1].length%2==0?(samples[1].length/2):((samples[1].length-1)/2);
        double sumBoth = (
                ((Math.pow(avg[0]+sigma[0]-avg[2], 2.0) )*sample1Len) +
                ((Math.pow(avg[0]-sigma[0]-avg[2], 2.0) )*sample1Len) +
                sample1Odd +
                ((Math.pow(avg[1]+sigma[1]-avg[2], 2.0) )*sample2Len) +
                ((Math.pow(avg[1]-sigma[1]-avg[2], 2.0) )*sample2Len) +
                sample2Odd)
        ;
        System.out.println("SumBoth=" + sumBoth);

        double sigmaBoth = Math.sqrt( (
                  sumBoth
                )
                /samples[2].length );
        System.out.println("foo: " + sample1Odd);
        System.out.println("bar: " + sample2Odd);

        System.out.println("Sigma both samples=" + sigmaBoth);

        if(sigmaBoth - sigma[2] == 0){
            System.out.println("Combined stdev of both samples are equal to stddev of merged samples.");
        }else{
            System.out.println("Combined stddev of both samples are not equal to stddev of merged samples, but should.");
        }

    }
}
