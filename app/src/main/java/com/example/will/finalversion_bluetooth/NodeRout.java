package com.example.will.finalversion_bluetooth;

/**
 * Created by Will on 03/08/2015.
 */
public class NodeRout {
    private String nodeName;
    private String nodeAddress;
    private int numberOfHops;
    private String nextNodeHop;
    private int sequenceNumber;

    NodeRout(String nodeAddress, int numberOfHops){
        this.nodeAddress=nodeAddress;
        this.numberOfHops=numberOfHops;
    }

    /**
     * for nodes that require MORE than 1 hop
     * @param nodeName
     * @param nodeAddress
     * @param numberOfHops
     * @param nextNodeHop
     */
    NodeRout(String nodeName, String nodeAddress,int numberOfHops,String nextNodeHop){
        this.nodeAddress=nodeAddress;
        this.numberOfHops=numberOfHops;
        this.nextNodeHop=nextNodeHop;
        this.nodeName=nodeName;
    }

    /**
     * for nodes that require ONLY 1 hop
     * @param nodeName
     * @param nodeAddress
     * @param numberOfHops
     */
    NodeRout(String nodeName, String nodeAddress,int numberOfHops){
        this.nodeAddress=nodeAddress;
        this.numberOfHops=numberOfHops;
        this.nodeName=nodeName;
    }

    public String getNodeName(){
        return  nodeAddress;
    }

    public String getNodeAddress(){
        return  nodeAddress;
    }

    public String getNextNodeHop(){
        return  nextNodeHop;
    }

    public int getNumberOfHops(){
        return  numberOfHops;
    }

    public void setNumberOfHops(int i){
        numberOfHops = i;
    }


}
