/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cs555.util;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author priyankb
 */
public class IDManipulation {

    public static int matchPrefix(String peerId1, String peerId2) {
        int indexToReturn = 0;
        int peerId1Length = peerId1.length();
        int peerId2Length = peerId2.length();
        int maxItrCount = peerId1Length < peerId2Length ? peerId1Length : peerId2Length;
        for (int i = 0; i < maxItrCount; i++) {
            if (peerId1.charAt(i) != peerId2.charAt(i)) {
                indexToReturn = i;
                break;
            }
        }
        return indexToReturn;
    }

//    private static int calculateDistance(String point, String neededPoint) {
//        int decimalDistance = (Integer.parseInt(neededPoint, 16) - Integer.parseInt(point, 16));
//
//        if (decimalDistance >= (65536 / 2)) {
////	System.out.println("Left");
////	System.out.println(decimalDistance - 65536);
//            return (decimalDistance - 65536);
//        } else {
////	System.out.println("Right");
////	System.out.println(decimalDistance);
//            return (decimalDistance);
//
//        }
//    }
    public static DistanceDirection getDistanceAndDirection(String node1Id, String node2Id) {
        int d1 = Integer.parseInt(node1Id, 16);
//        System.out.println(d1);
        int d2 = Integer.parseInt(node2Id, 16);
//        System.out.println(d2);
        int diff1 = d1 - d2;
        int absDiff = Math.abs(diff1);
//        System.out.println(absDiff);
//        System.out.println((65536 - absDiff));
        char dir;
        int ans = 0;
        if (d1 < d2) {
            if (absDiff < (65536 - absDiff)) {
                dir = Protocol.DIRECTION.RIGHT;
                ans = absDiff;
            } else {
                dir = Protocol.DIRECTION.LEFT;
                ans = 65536 - absDiff;
            }
        } else {
            if (absDiff < (65536 - absDiff)) {
                dir = Protocol.DIRECTION.LEFT;
                ans = absDiff;
            } else {
                dir = Protocol.DIRECTION.RIGHT;
                ans = 65536 - absDiff;
            }
        }
        return new DistanceDirection(dir, ans);
    }

    public static String nearerNode(String node, String node1, String node2) {
        DistanceDirection dd1 = IDManipulation.getDistanceAndDirection(node, node1);
        DistanceDirection dd2 = IDManipulation.getDistanceAndDirection(node, node2);

        int d1 = dd1.getDistance();
        int d2 = dd2.getDistance();

        String nearerNode = (d1 < d2) ? node1 : node2;
        if (d1 == d2) {
            if (dd1.getDirection() == Protocol.DIRECTION.RIGHT) {
                nearerNode = node1;
            } else if (dd2.getDirection() == Protocol.DIRECTION.RIGHT) {
                nearerNode = node2;
            }
        }

        return nearerNode;
    }

    public static String filenametoKey(String filename) {

        String key = null;
        try {
            String encoded = EncodeSHA1.SHA1(filename);
            key = encoded.substring(0, 4);
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(IDManipulation.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(IDManipulation.class.getName()).log(Level.SEVERE, null, ex);
        }
        return key;
    }

    public static boolean isHEX(String hexString) {
        if (hexString.length() == 0
                || (hexString.charAt(0) != '-' && Character.digit(hexString.charAt(0), 16) == -1)) {
            return false;
        }
        if (hexString.length() == 1 && hexString.charAt(0) == '-') {
            return false;
        }

        for (int i = 1; i < hexString.length(); i++) {
            if (Character.digit(hexString.charAt(i), 16) == -1) {
                return false;
            }
        }
        return true;
    }

    public static int countDistanceInDirection(char direction, String node1Id, String node2Id) {
        int distance = 0;
        int d1 = Integer.parseInt(node1Id, 16);
        int d2 = Integer.parseInt(node2Id, 16);

        switch (direction) {
            case Protocol.DIRECTION.LEFT:
                if (d1 < d2) {
                    distance = 65536 - d2 + d1;
                } else {
                    distance = d1 - d2;
                }
                break;

            case Protocol.DIRECTION.RIGHT:
                if (d1 < d2) {
                    distance = d2 - d1;
                } else {
                    distance = 65536 - d1 + d2;
                }
                break;
        }
        return distance;
    }

    public static class DistanceDirection {

        char direction;
        int distance;

        private DistanceDirection(char dir, int dist) {
            this.direction = dir;
            this.distance = dist;
        }

        public char getDirection() {
            return direction;
        }

        public void setDirection(char direction) {
            this.direction = direction;
        }

        public int getDistance() {
            return distance;
        }

        public void setDistance(int distance) {
            this.distance = distance;
        }
    }

}
