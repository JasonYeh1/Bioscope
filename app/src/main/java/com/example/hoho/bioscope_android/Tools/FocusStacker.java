package com.example.hoho.bioscope_android.Tools;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.opencv.android.Utils;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.KeyPoint;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.DMatch;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.core.TermCriteria;
import org.opencv.features2d.BFMatcher;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.Feature2D;
import org.opencv.features2d.Features2d;
import org.opencv.features2d.ORB;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.photo.AlignMTB;

import static org.opencv.core.Core.addWeighted;
import static org.opencv.photo.Photo.createAlignMTB;
import static org.opencv.video.Video.findTransformECC;

/**
 * A simple focus stacking algorithm using java and openCV
 *
 * Steps :
 * https://stackoverflow.com/questions/15911783/what-are-some-common-focus-stacking-algorithms
 *
 * Thanks to Charles McGuinness (python exemple)
 *
 * @author Lucas Lelaidier
 */
public class FocusStacker
{
    /**
     * List of images to merge together
     */
    private ArrayList<Mat> inputs = new ArrayList<>();

    /**
     * Path to the folder which contains the images
     */
    private String path;

    public FocusStacker(String path)
    {
        this.path = path.replace("\\", "/");
    }

    //Constructor that takes in a string path which points to a file location within the Android device where the images are held.
    //Also will use this location to put output image
    //inputs: ArrayList of Mats which represent the images chosen for focus stacking
    public FocusStacker(String path, ArrayList<Mat> inputs)
    {
        this.path = path.replace("\\", "/");
        this.inputs = inputs;
    }

    //Default constructor, unused
    public FocusStacker(ArrayList<Mat> inputs) {
        this.inputs = inputs;
    }

    //Method to set the ArrayList<Mat> inputs to a custom input ArrayList
    public void setInputs(ArrayList<Mat> inputs)
    {
        this.inputs = inputs;
    }

    /**
     * Method that uses the keypoints method of aligning images.
     * One way of aligning images is to find key points of each images and match them together.
     * When we have two keypoints that match, the corresponding distance between the two points can be used to calculate the homography of the transition.
     * We can use this homography in a matrix multiplication to morph one image to be aligned to the other.
     *
     * This method does not work in our implementation because finding matching keypoints between two photos of different focus is difficult.
     * One image may have a keypoint, but the other image may have that keypoint blurred out.
     *
     * @param (image_1_kp) Contains a Mat object which houses the keypoints of image 1
     * @param (image_2_kp) Contains a Mat object which houses the keypoints of image 2
     * @param (matches)      Contains the matching keypoints between image 1 and image 2
     * @return returns a Mat homography which is used to transform the image for alignment
     */
//    public Mat findHomography(MatOfKeyPoint image_1_kp, MatOfKeyPoint image_2_kp, MatOfDMatch matches) {
//        List<DMatch> matchesList = matches.toList();
//        Double max_dist = 0.0;
//        Double min_dist = 100.0;
//        /*Collections.sort(matchesList,new Comparator<DMatch>() {
//                @Override
//                public int compare(DMatch o1, DMatch o2) {
//                    if(o1.distance<o2.distance)
//                        return -1;
//                    if(o1.distance>o2.distance)
//                        return 1;
//                    return 0;
//                }
//            });
//            if(matchesList.size()>128){
//                matchesList = matchesList.subList(0,128);
//            }*/
//        for(int i = 0; i < matchesList.size(); i++) {
//            double dist = (double)matchesList.get(i).distance;
//            if(dist < min_dist)
//                min_dist = dist;
//            if(dist > max_dist)
//                max_dist = dist;
//        }
//        System.out.println("min dist: " + min_dist);
//        System.out.println("max dist: " + max_dist);
//
//        List<DMatch> goodMatches = new LinkedList<>();
//        for(int i = 0; i < matchesList.size(); i++) {
//            if(matchesList.get(i).distance <= 5 * min_dist)
//                goodMatches.add(matchesList.get(i));
//        }
//        if(goodMatches.size() > 128) {
//            goodMatches = goodMatches.subList(0,10);
//        }
//
//        LinkedList<Point> image_1_points = new LinkedList<>();
//        LinkedList<Point> image_2_points = new LinkedList<>();
//
//        List<KeyPoint> keypoints1 = image_1_kp.toList();
//        List<KeyPoint> keypoints2 = image_2_kp.toList();
//
//        /*for(int i = 0; i < matchesList.size(); i++) {
//            image_1_points.addLast(keypoints1.get(matchesList.get(i).queryIdx).pt);
//            image_2_points.addLast(keypoints2.get(matchesList.get(i).trainIdx).pt);
//        }*/
//
//        for( int i = 0; i < goodMatches.size(); i++) {
//            image_1_points.addLast(keypoints1.get(goodMatches.get(i).queryIdx).pt);
//            image_2_points.addLast(keypoints2.get(goodMatches.get(i).trainIdx).pt);
//        }
//        MatOfDMatch gm = new MatOfDMatch();
//        gm.fromList(goodMatches);
//
//        MatOfPoint2f obj = new MatOfPoint2f();
//        obj.fromList(image_1_points);
//
//        MatOfPoint2f scene = new MatOfPoint2f();
//        scene.fromList(image_2_points);
//
//        //Testing keypoints, matches, and drawing correct matches
//        Mat first = inputs.get(0);
//        Mat second = inputs.get(1);
//        List<Mat> src = Arrays.asList(first, second);
//        Mat combined = new Mat();
//        Core.hconcat(src,combined);
//        MatOfByte drawnMatches = new MatOfByte();
//        Features2d.drawMatches(inputs.get(0), image_1_kp, inputs.get(1), image_2_kp, gm, combined, Scalar.all(-1),Scalar.all(-1),drawnMatches,Features2d.DRAW_OVER_OUTIMG);
//
//        Mat color = new Mat();
//        Imgproc.cvtColor(combined, color, Imgproc.COLOR_BGR2RGB);
//        String timestamp = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
//        String prepend = "MATCHES_" + timestamp + "_";
//        Imgcodecs.imwrite(path + "/"  + prepend + ".png", color);
//
//        Mat h = Calib3d.findHomography(scene, obj, Calib3d.RANSAC, 2);
//
//        return h;
//    }

    /**
     * Method that uses transformECC algorithm to align images together. This algoritm works because it
     * continuously estimates the difference between images to eventually arrive at a proper homography.
     * However this algorithm can take a long time to arrive at a solution. Possibly altering the iterations variable
     * and termination criteria variable can possibly lower the time at the cost of an incorrect homography.
     * Another solution may be to use image pyramids to help the homography prediction further
     *
     * @param (images) Contains an ArrayList of Mats. Represents the images to be aligned together
     * @return returns a Mat homography which is used to transform the image for alignment
     */
    private ArrayList<Mat> alignImagesECC(ArrayList<Mat> images) {
        System.out.println("Beginning ECC");

//      MOTION_TRANSLATION = 0, warpMatrix is 2X3 with the first 2X2 part being the unity matrix and the rest two parameters being estimated.
//      MOTION_EUCLIDEAN = 1,   three parameters are estimated; warpMatrix is 2X3
//      MOTION_AFFINE = 2,      six parameters are estimated; warpMatrix is 2X3.
//      MOTION_HOMOGRAPHY = 3   eight parameters are estimated; warpMatrix is 3X3.

        int warp_mode = 3;
        ArrayList<Mat> outImages = new ArrayList<>();
        //Assigning first image to image 1, this first image will be what the rest of the
        //images will be aligned to
        Mat im1 = images.get(0);
        outImages.add(im1);

        //Grayscale the image for better results
        Mat images1gray = new Mat();
        Imgproc.cvtColor(im1, images1gray, Imgproc.COLOR_BGR2GRAY);

        //For loop to iterate through the rest of the images, applying the findTransformECC algorithm
        //on each image. Each image will be compared to the first image set above.
        for(int i = 1; i < images.size(); i++) {
            Mat im2 = images.get(i);
            Mat image_i_gray = new Mat();
            Imgproc.cvtColor(images.get(i), image_i_gray, Imgproc.COLOR_BGR2GRAY);

            //Using MOTION_HOMOGRAPHY warp mode to allow for better translations
            Mat warp_matrix;
            if(warp_mode == 3){
                //Since warp mode is 3, we need to create a warp_matrix wit the correct size
                //CvType explanation: https://stackoverflow.com/questions/13428689/whats-the-difference-between-cvtype-values-in-opencv
                warp_matrix = Mat.eye(3,3, CvType.CV_32F);
            } else {
                warp_matrix = Mat.eye(2,3, CvType.CV_32F);
            }

            //Number of maximum iterations for the algorithm
            int iterations = 100;

            //Termination value of algorithm
            double termination = 1e-6;
            //Object TermCriteria is created which takes a type int value, number of iterations, and the termination value
            TermCriteria criteria = new TermCriteria(TermCriteria.COUNT+TermCriteria.EPS, iterations,termination);

            //Method to execute the algorithm. image 1, image i, emtpy warp matrix, TerminationCriteria
            findTransformECC(images1gray, image_i_gray, warp_matrix, warp_mode, criteria);

            //Create empty Mat object to become final image Mat
            Mat im2_aligned = new Mat();
            if(warp_mode == 3) {
                //Applies the homography onto the photo to warp it for alignment
                Imgproc.warpPerspective(im2,im2_aligned, warp_matrix, im1.size(), Imgproc.INTER_LINEAR + Imgproc.WARP_INVERSE_MAP);
            } else {
                Imgproc.warpAffine(im2, im2_aligned, warp_matrix, im1.size(), Imgproc.INTER_LINEAR + Imgproc.WARP_INVERSE_MAP, Core.BORDER_DEFAULT);
            }

            outImages.add(im2_aligned);
        }
        System.out.println("ECC END");
        return outImages;
    }


//    public ArrayList<Mat> align_images(ArrayList<Mat> images){
//        ArrayList<Mat> outImages = new ArrayList<>();
//        ORB detector = ORB.create(1000);
//
//        outImages.add(images.get(0));
//        Mat image1gray = new Mat();
//        Imgproc.cvtColor(images.get(0),image1gray, Imgproc.COLOR_BGR2GRAY);
//
//        MatOfKeyPoint image_1_kp = new MatOfKeyPoint();
//        Mat image_1_desc = new Mat();
//        detector.detectAndCompute(images.get(0), new Mat() , image_1_kp, image_1_desc);
//
//        for(int i=1; i<images.size(); i++) {
//            //Log.d(TAG, "Aligning image");
//            MatOfKeyPoint image_i_kp = new MatOfKeyPoint();
//            Mat image_i_desc = new Mat();
//            detector.detectAndCompute(images.get(i), new Mat() , image_i_kp, image_i_desc);
//
//            DescriptorMatcher bf = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);
//            MatOfDMatch matches = new MatOfDMatch();
//            bf.match(image_1_desc, image_i_desc, matches);
//
//            Mat hom = findHomography(image_1_kp, image_i_kp, matches);
//            Mat newImage = new Mat();
//            Imgproc.warpPerspective(images.get(i), newImage, hom, images.get(0).size(),1,Core.BORDER_TRANSPARENT);
//
//            outImages.add(newImage);
//
//        }
//        return outImages;
//    }

    /**
     * Compute the gradient map of the image
     * @param image image to transform
     * @return image image transformed
     */
    public Mat laplacien(Mat image, int i)
    {
        //YOU SHOULD TUNE THESE VALUES TO SUIT YOUR NEEDS
        int kernel_size = 5;        //Size of the laplacian window
        double blur_size = 5;       //How big of a kernal to use for the gaussian blur
                                    //Generally, keeping these two values the same or very close works well
                                    //Also, odd numbers, please...

        //Grayscales the image for better results
        Mat gray = new Mat();
        Imgproc.cvtColor(image, gray, 6);
        System.out.println("cvtColor Done");

        //Applies Gaussian Blur to reduce image noise
        Mat gauss = new Mat();
        Imgproc.GaussianBlur(gray, gauss, new Size(blur_size, blur_size), 0);
        System.out.println("GaussianBlur Done");

        /*String timestamp = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
        String prepend = "blur" + timestamp + "_";
        Imgcodecs.imwrite(path + "/"  + prepend + ".png", gauss);*/

        //Applies laplacian filter to find the sharpest portions of the image
        Mat laplace = new Mat();
        Imgproc.Laplacian(gauss, laplace, CvType.CV_32F, kernel_size, 1, 0, Core.BORDER_DEFAULT);

        /*String timestamp2 = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
        String prepend2 = "Laplace" + i + timestamp2 + "_";
        Imgcodecs.imwrite(path + "/"  + prepend2 + ".jpg", laplace);*/

        //Applies Bilateral Filter to further reduce the noise of image
        Mat smooth = new Mat();
        Imgproc.bilateralFilter(laplace,smooth,32,75,75);

        /*String timestamp = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
        String prepend = "Smooth"  + i + timestamp + "_";
        Imgcodecs.imwrite(path + "/"  + prepend + ".jpg", smooth);*/

        //Converts the image into an absolute scaled image
        Mat absolute = new Mat();
        Core.convertScaleAbs(smooth, absolute);

        return absolute;
    }

    /**
     * apply focus stacking on inputs
     */
    public void focus_stack()
    {
        //findTransformECC
        inputs = alignImagesECC(inputs);

        //findHomography
        //inputs = align_images(inputs);

        /*System.out.println(inputs.size());
        Mat color1 = new Mat();
        Imgproc.cvtColor(inputs.get(1), color1, Imgproc.COLOR_BGR2RGB);
        String timestamp1 = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
        String prepend1 = "MERGED_IMAGE_" + timestamp1 + "_";
        Imgcodecs.imwrite(path + "/"  + prepend1+ ".png", color1);*/
        //Case: no images in array
        if(inputs.size() == 0)
        {
            System.out.println("please select some inputs");
        }
        else
        {
            System.out.println("Computing the laplacian of the blurred images");
            Mat[] laps = new Mat[inputs.size()];

            //Main loop to prepare images for image alignment and focus stacking
            for (int i = 0 ; i < inputs.size() ; i++)
            {
                System.out.println("image "+i);
                laps[i] = laplacien(inputs.get(i), i+1);
            }
            System.out.println("Laplacien Done");

            //Create an empty Mat filled with just zeros
            Mat vide = Mat.zeros(laps[0].size(), inputs.get(0).type());

            int columns = laps[0].cols();
            int rows    = laps[0].rows();
            int len     = laps.length;
            int index   = -1;

            //Triple for loop to step through the x and y position of every single image.
            //It may seem like this method is the one taking the longest because it is n^3
            //However tests show that findTransformECC is what causes the lengthy run time.
            for(int y = 0 ; y < columns ; y++)
            {
                for(int x = 0 ; x < rows ; x++)
                {
                    //int index = -1;
                    double indexValue = -1;
                    for (int i = 0 ; i < len ; i++)
                    {
                        //Choose the sharpest pixel
                        double val = laps[i].get(x,y)[0];
                        if(val > indexValue)
                        {
                            indexValue = val;
                            index = i;
                        }
                    }
                    //Put the sharpest pixel inside the empty Mat
                    vide.put(x, y, inputs.get(index).get(x, y));
                }
            }
            //Revert the color back to BGR. Android uses BGR not RGB
            Mat color = new Mat();
            Imgproc.cvtColor(vide, color, Imgproc.COLOR_BGR2RGB);
            System.out.println("Success !");

            //Give the image a unique name by using the current time and write the image to the path directory
            String timestamp = new SimpleDateFormat("yyMMdd-HHmmss").format(new Date());
            String prepend = "MERGED_IMAGE_" + timestamp + "_";
            Imgcodecs.imwrite(path + "/"  + prepend + ".jpg", color);
        }
    }
}








