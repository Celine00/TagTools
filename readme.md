## 照片标注程序使用说明

> Created By Mingkuan At 2017-04-17

- *可以直接执行系统脚本的方式运行程序**

```shell
#shell 打开照片标注程序
#参数分别代表：图片文件夹 输出文件夹 地标label文件 fast-rcnn的标注结果
samples/image samples/output samples/landmark_list.txt samples/reference

#C++调用系统命令
system(command)

#Java调用系统命令
Process process = Runtime.getRuntime.exec(command);
process.waitFor();
```

![1](images/1.png)

- 其中三个参数的含义分别是：

| parameters |          samples          |   meanings   |
| :--------: | :-----------------------: | :----------: |
|   arg[0]   |     samples/image         |   图片文件夹   |
|   arg[1]   |    samples/output         |   输出文件夹   |
|   arg[2]   | samples/landmark_list.txt | 店铺序号与名字对应的列表 |
|   arg[3]   |      samples/reference    |  fast-rcnn的标注结果 |

- imageList.txt内有两列，第一列为图片文件夹下所有图片的名称，第二列标记了该图片是否已标注，0-未标注，1-已标注；每次打开程序会从第一张未标注图片开始往下遍历

- 结果文件的文件名称与图片名称保持一致。
- 结果文件一共有8列，每一行代表在照片里的一个landmark，每一列的含义分别是：

```shell
#输入图片文件名 #左上角坐标x y #右下角的坐标x y #该区域对应的店铺编号 #该区域是否模糊 #该区域是否完整
GOPR0742.JPG 219 1134 779 1384 2 0 0
GOPR0742.JPG 1614 1259 2484 1554 3 0 1
GOPR0742.JPG 3089 1639 3234 1714 4 1 0
```

- 店铺序号与名字对应的列表一共有两列，每一行代表一个店铺，第一列代表店铺编号，第二列表示店铺的名字，这个列表为了方便在标注的时候确定对应的举行区域是属于哪个店铺的

#### 标注过程

1. 在左侧图片上通过鼠标框出店铺招牌区域，然后在右边输入框输入该Landmark对应的序号，然后点击Save保存的当前的矩形区域，保存后的矩形区域由红色变成蓝色
2. 如果标注错误，可以点击Clear清空当前图片上的所有区域
3. 标注完当前图片的区域后，点击Next保存标注信息到输出文件并显示下一张图片

注意：需要点击“下一个”按钮才会标记当前图片已标注，否则下次运行程序仍从上次标记的最后一张开始。


![2](images/2.png)
# TagTool
