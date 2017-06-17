package me.demetoir.a3dsound_ndk;

import java.util.Random;

class Point2D {
    float x;
    float y;

    Point2D() {
        this.x = 0;
        this.y = 0;
    }


    Point2D(float x, float y) {
        this.x = x;
        this.y = y;
    }

    Point2D(double x, double y) {
        this.x = (float) x;
        this.y = (float) y;
    }

    Point2D add(Point2D p) {
        Point2D newP = new Point2D();
        newP.x = this.x + p.x;
        newP.y = this.y + p.y;
        return newP;
    }

    Point2D add(double x, double y) {
        Point2D newP = new Point2D();
        newP.x = (float) (this.x + x);
        newP.y = (float) (this.y + y);
        return newP;
    }

    Point2D add(float x, float y) {
        Point2D newP = new Point2D();
        newP.x = this.x + x;
        newP.y = this.y + y;
        return newP;
    }

    Point2D sub(Point2D p) {
        Point2D newP = new Point2D();
        newP.x = this.x - p.x;
        newP.y = this.y - p.y;
        return newP;
    }

    Point2D sub(float x, float y) {
        Point2D newP = new Point2D();
        newP.x = this.x - x;
        newP.y = this.y - y;
        return newP;
    }

    Point2D sub(double x, double y) {
        Point2D newP = new Point2D();
        newP.x = (float) (this.x - x);
        newP.y = (float) (this.y - y);
        return newP;
    }

    double distance(Point2D p) {
        double dx = this.x - p.x;
        double dy = this.y - p.y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    double angle(Point2D p) {
        return Math.atan2(this.y - p.y, this.x - p.x);
    }


    void randomize(int left, int top, int right, int bottom) {
        Random random = new Random();
        this.x = random.nextInt(right - left) - (right - left)/2;
        this.y = random.nextInt(bottom - top) - (bottom - top)/2;
    }
}
