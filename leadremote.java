//Gabe Brown
//Hack Generation Y January 24-25 2015
//gets input from Leap-Motion and uses JSch to SSH to Raspberry Pi
//needs Leap-Motion SDK and JSch API

import java.io.*;
import com.leapmotion.leap.*;
import com.jcraft.jsch.*;
import java.awt.*;
import javax.swing.*;
import java.util.Properties;

class SampleMe {

   public static void main(String[] args) {
      Controller controller = new Controller();
      SampleListener listener = new SampleListener();
      
      controller.addListener(listener);
      
      System.out.println("Press Enter to quit...");
      try {
         System.in.read();
      } 
      catch (IOException e) {
         e.printStackTrace();
      }
      
      controller.removeListener(listener);
   }
   
   public static void transmit(int button){
      String command = "";
      
      if (button == 1) {
         command="irsend SEND_ONCE lirc-pda.conf KEY_RIGHT";
      } 
      else if (button == 2) {
         command="irsend SEND_ONCE lirc-pda.conf KEY_PLAYPAUSE";
      } 
      else if (button == 3) {
         command="irsend SEND_ONCE lirc-pda.conf KEY_LEFT";
      } 
      else if (button == 4) {
         command="irsend SEND_START lirc-pda.conf KEY_RIGHT";
      }
      else if (button == 5) {
         command="irsend SEND_START lirc-pda.conf KEY_LEFT";
      }
      else if (button == 6) {
         command="irsend SEND_STOP lirc-pda.conf KEY_RIGHT";
      }
      else if (button == 7) {
         command="irsend SEND_STOP lirc-pda.conf KEY_LEFT";
      }
      try {
         JSch js = new JSch();
         Session s = js.getSession("pi", "192.168.1.120", 22);
         s.setPassword("raspberry");
         Properties config = new Properties();
         config.put("StrictHostKeyChecking", "no");
         s.setConfig(config);
         s.connect();
      
         Channel c = s.openChannel("exec");
         ChannelExec ce = (ChannelExec) c;
      
         ce.setCommand(command);
         ce.setErrStream(System.err);
      
         ce.connect();
      
         BufferedReader reader = new BufferedReader(new InputStreamReader(ce.getInputStream()));
         String line;
         while ((line = reader.readLine()) != null) {
            System.out.println(line);
         }
         
         System.out.println(button);
      
         ce.disconnect();
         s.disconnect();
      }
      catch(Exception e){
         System.out.println(e);
      }
   }
}

class SampleListener extends Listener {
   
   public void onInit(Controller controller) {
      System.out.println("Initialized");
   }   
   public void onConnect(Controller controller) {
      System.out.println("Connected");
      controller.enableGesture(Gesture.Type.TYPE_SWIPE);
      controller.enableGesture(Gesture.Type.TYPE_CIRCLE);
      controller.enableGesture(Gesture.Type.TYPE_KEY_TAP);
   }
   
   public static void delay(int milSec) {
      try {
         Thread.sleep(milSec);                
      } 
      catch(InterruptedException ex) {
         Thread.currentThread().interrupt();
      }
   }
   
   public void onFrame(Controller controller) {
      com.leapmotion.leap.Frame frame = controller.frame();
      Hand hand = frame.hands().rightmost();
      GestureList curGestures = frame.gestures();
      controller.config().setFloat("Gesture.Swipe.MinLength", 175f);
      controller.config().setFloat("Gesture.Swipe.MinVelocity", 100f);
      controller.config().setFloat("Gesture.Circle.MinRadius", 5.0f);
      controller.config().setFloat("Gesture.Circle.MinArc", 4.0f);
      controller.config().setFloat("Gesture.KeyTap.MinDownVelocity", 1.0f);
      controller.config().setFloat("Gesture.KeyTap.MinDistance", .5f);
      controller.config().save();
      for(int i = 0; i < curGestures.count(); i++) {
         if (curGestures.get(i).type() == Gesture.Type.TYPE_CIRCLE) {
            CircleGesture cGest = new CircleGesture(curGestures.get(i));
            if (cGest.progress() > 3) {
               if (cGest.pointable().direction().angleTo(cGest.normal()) <= Math.PI/2){
                  System.out.println("fast forward");
                  SampleMe.transmit(4);
                  delay(4000);
                  SampleMe.transmit(6);
               } 
               else {
                  System.out.println("rewind");
                  SampleMe.transmit(5);
                  delay(1500);
                  SampleMe.transmit(7);
               }
            }
         }
         else if (curGestures.get(i).type() == Gesture.Type.TYPE_KEY_TAP) {
            SampleMe.transmit(2);
            System.out.println("tap");
            delay(1000);
         } 
         else if (curGestures.get(i).type() == Gesture.Type.TYPE_SWIPE) {
            SwipeGesture swGest = new SwipeGesture(curGestures.get(i));
            Vector swDir = swGest.direction();
            if (swDir.getX() < 0 && (swDir.getY() < .5 && swDir.getY() > -.5)) {
               SampleMe.transmit(3);
               System.out.println("Swipe Down");
               delay(1500);
            } 
            else if (swDir.getX() > 0 && (swDir.getY() < .5 && swDir.getY() > -.5)) {
               SampleMe.transmit(1);
               System.out.println("Swipe Up");
               delay(1200);
            }
         }  
         else if (hand.palmVelocity().getY() > 1.2 || hand.palmVelocity().getY() < -1.2) {
            if (hand.palmVelocity().getY() > 1.2) {
               System.out.println("Volume Up");
               delay(1500);
            } 
            else if (hand.palmVelocity().getY() < -1.2) {
               System.out.println("Volume Down");
               delay(1500);
            }
         }    
      }
   }
}
