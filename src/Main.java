import json.JSONObject;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * Created by limkuan on 16/9/21.
 */
public class Main {
    private JPanel mMainPanel;
    private JTextField mLandmarkNum;
    private JButton mSaveButton;
    private JButton mClearButton;
    private TagPanel mImagePanel;
    private JLabel mLandmarkLabel;
    private JButton mNextButton;
    private JCheckBox mDifficultCheckBox;
    private JCheckBox mTruncatedCheckBox;

    private Map<String, String> mLabelList;
    private List<TagLabel> mReferenceLabels;

    private String mOutputFilename;
    private String mFrameImagePath;

    private List<String> mImageNameList;
    private List<String> mImageFlag;
    private static List<String> inputArguments;
    private int index_current = -1;

    private Main() {
        mNextButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveLabels(mImagePanel.getLabelList());
                index_current++;
                System.out.println("++"+Integer.toString(mImageNameList.size()));

                if (index_current < mImageNameList.size()) {
                    // find the next image not handled
                    updateInterface(inputArguments.get(0),inputArguments.get(1),inputArguments.get(2),inputArguments.get(3),index_current);
                    System.out.println("--"+Integer.toString(index_current));
                    System.out.println("||"+inputArguments.get(0) + "/" + mImageNameList.get(index_current) + ".jpg");
                    mImagePanel.nextImage(inputArguments.get(0) + "/" + mImageNameList.get(index_current) + ".jpg");
                } else {
                    // alert
                    JOptionPane.showMessageDialog(null,
                            "No more image", "Error Massage",
                            JOptionPane.ERROR_MESSAGE);
                    System.exit(0);
                }
            }
        });

        mLandmarkNum.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {
                warn();
            }

            public void removeUpdate(DocumentEvent e) {
                warn();
            }

            public void insertUpdate(DocumentEvent e) {
                warn();
            }

            void warn() {
                mLandmarkLabel.setText(mLabelList.get(mLandmarkNum.getText()));
            }
        });

        mSaveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    boolean is_truncated = mTruncatedCheckBox.isSelected();
                    boolean is_difficult = mDifficultCheckBox.isSelected();
                    mImagePanel.saveCurrentLabel(Integer.valueOf(mLandmarkNum.getText()),
                            is_truncated, is_difficult);
                    mImageFlag.set(index_current, "1");
                    saveImageList(inputArguments.get(1));
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(mMainPanel,
                            "Error: Landmark number must be a number", "Error Massage",
                            JOptionPane.ERROR_MESSAGE);
                    ex.printStackTrace();
                }
            }
        });

        mClearButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                mImagePanel.clearLabel();
            }
        });

        mReferenceLabels = new ArrayList<>();
        mLabelList = new HashMap<>();

        mImageNameList = new LinkedList<>();
        mImageFlag = new LinkedList<>();

        inputArguments = new LinkedList<>();
    }

    public static void main(String[] args) {
        /**
         * args[0] the image folder
         * args[1] the output folder
         * args[2] the landamrk list path
         * args[3] the reference folder
         *
         * output file format
         * frame filename | left_x | left_y | right_x | right_y | landmark_id
         */

        if (args.length < 3) {
            JOptionPane.showMessageDialog(null,
                    "Error: run this app with three arguments", "Error Massage",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        Main main = new Main();
        JFrame frame = new JFrame("Main");
        frame.setContentPane(main.mMainPanel);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setSize(main.mMainPanel.getPreferredSize().width, main.mMainPanel.getPreferredSize().height + 50);
        frame.setResizable(false);
        frame.setVisible(true);

        inputArguments.add(args[0]);
        inputArguments.add(args[1]);
        inputArguments.add(args[2]);
        inputArguments.add(args[3]);

        main.setImageList(args[0],args[1]);
        main.updateInterface(args[0],args[1],args[2],args[3],0);
        main.nextImage();
    }

    private void nextImage() {
        if (mFrameImagePath != null) {
            mImagePanel.nextImage(mFrameImagePath);
        }
    }

    private void createUIComponents() {

        mImagePanel = new TagPanel();

        TagPanel.onLandmarkRegionChangeListener listener = new TagPanel.onLandmarkRegionChangeListener() {
            @Override
            public void onLandmarkRegionChange(TagLabel label) {
                System.out.printf(Locale.ENGLISH, "rectangle: %d %d %d %d\n",
                        label.left_x, label.left_y, label.right_x, label.right_y);
                float maxIoU = 0;
                int maxIndex = 0;
                for (TagLabel l : mReferenceLabels) {
                    float iou = l.calculateIoU(label);
                    if(iou > maxIoU) {
                        maxIoU = iou;
                        maxIndex = l.landmark_num;
                    }
                }
                if(maxIoU != 0) {
                    mLandmarkNum.setText(String.format("%d", maxIndex));
                }
                mLandmarkNum.requestFocus();
                mLandmarkNum.selectAll();
            }
        };
        mImagePanel.setListener(listener);
    }

    private void setImageList(String imagePath, String outputPath) {
        try {
            // get file list according to the path
            File file = new File(imagePath);
            // get the file list
            File[] array = file.listFiles();

            // search imageList.txt
            File _imageList = new File(outputPath + "/imageList.txt");
            //searchFile(array, "imageList.txt");

            if (!_imageList.exists()) {
                _imageList.createNewFile();
                PrintStream ps = new PrintStream(_imageList);
                for(int i=0;i<array.length;i++){
                    if(array[i].isFile() && array[i].getName().endsWith(".jpg")){
                        mImageFlag.add("0");
                        // remove the suffix
                        int dot = array[i].getName().lastIndexOf('.');
                        if ((dot >-1) && (dot < (array[i].getName().length()))) {
                            mImageNameList.add(array[i].getName().substring(0, dot));
                            ps.printf("%s %d\n", array[i].getName().substring(0, dot), 0);
                        }
                    }
                }
                ps.flush();
                ps.close();
            } else {
                Scanner scanner = new Scanner(_imageList);
                while (scanner.hasNext()) {
                    String line = scanner.nextLine();
                    mImageNameList.add(line.split(" ")[0]);
                    mImageFlag.add(line.split(" ")[1]);
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }

    private void updateInterface(String imagePath, String outputPath, String labelListPath, String referencePath, int init_index) {
        // find the first image not handled
        index_current = -1;
        for(int i=init_index;i<mImageFlag.size();i++){
            if (mImageFlag.get(i).equals("0")) {
                index_current = i;
                break;
            }
        }
        if (index_current == -1) {
            // alert
            JOptionPane.showMessageDialog(null,
                    "No more image", "Error Massage",
                    JOptionPane.ERROR_MESSAGE);
            System.exit(0);

        } else {
            this.setFrameImagePath(imagePath + "/" + mImageNameList.get(index_current) + ".jpg");
            this.setOutputFilename(outputPath + "/" + mImageNameList.get(index_current) + ".txt");
            this.setLandmarkList(labelListPath);
            this.setReferenceRegion(referencePath + "/" + mImageNameList.get(index_current) + ".json");
        }
    }

    private void saveImageList(String outputPath) {
        try {
            List<String> lines = Files.readAllLines(Paths.get(outputPath + "/imageList.txt")); // 读取文本文件
            lines.remove(index_current);
            lines.add(index_current, mImageNameList.get(index_current) + " " + mImageFlag.get(index_current));
            Files.write(Paths.get(outputPath + "/imageList.txt"), lines);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    private void setLandmarkList(String labelList) {
        try {
            Scanner scanner = new Scanner(new File(labelList));
            int count = 1;
            while (scanner.hasNext()) {
                String line = scanner.nextLine();
                mLabelList.put(line.split(" ")[0], line);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void setReferenceRegion(String referenceFile) {
        try {
            Scanner scanner = new Scanner(new File(referenceFile));
            StringBuilder json = new StringBuilder();
            while (scanner.hasNext()) {
                String line = scanner.nextLine();
                json.append(line);
            }
            JSONObject object = new JSONObject(json.toString());
            for (int i = 0; i < object.getInt("object_num"); i++) {
                JSONObject rect = object.getJSONArray("objects").getJSONObject(i);
                float xmin = rect.getJSONObject("bndbox").getFloat("xmin");
                float ymin = rect.getJSONObject("bndbox").getFloat("ymin");
                float xmax = rect.getJSONObject("bndbox").getFloat("xmax");
                float ymax = rect.getJSONObject("bndbox").getFloat("ymax");
                TagLabel label = new TagLabel((int) xmin, (int) ymin, (int) xmax, (int) ymax);
                int landmark_num = rect.getString("id").equals("undefined") ? -1 : Integer.parseInt(rect.getString("id"));
                label.setLandmarkNum(landmark_num);
                mReferenceLabels.add(label);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void setOutputFilename(String mOutputFilename) {
        this.mOutputFilename = mOutputFilename;
    }

    private void setFrameImagePath(String mFrameImagePath) {
        this.mFrameImagePath = mFrameImagePath;
    }

    private void saveLabels(ArrayList<TagLabel> labels) {
        try {
            PrintStream ps = new PrintStream(new File(mOutputFilename));

            for (TagLabel label : labels) {
                /*
                  output format
                  frame filename | left_x | left_y | right_x | right_y | landmark_id
                 */
                ps.printf("%s %d %d %d %d %d %d %d\n", mFrameImagePath,
                        label.left_x, label.left_y, label.right_x,
                        label.right_y, label.landmark_num,
                        label.is_truncated ? 1 : 0, label.is_difficult ? 1 : 0);
            }

            ps.flush();
            ps.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
