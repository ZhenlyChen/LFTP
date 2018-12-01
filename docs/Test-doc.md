# LFTP - Test doc

测试文档

## 局域网测试

### 服务端

开启服务：

```bash
$ java -jar .\lftp_main.jar server
```

默认端口为`3000`，默认数据文件夹为`./serverData`，默认地址池为`20480-20489`

![1543639228867](Test-doc/1543639228867.png)

### 客户端

#### 发送数据

查看帮助信息

```bash
$ java -jar .\lftp_main.jar lsend -h
```

![1543640945364](Test-doc/1543640945364.png)



先发送一个4M大小的mp3文件

```bash
$ java -jar .\lftp_main.jar lsend -s 127.0.0.1:3000 ./data/music.mp3
```

![1543639563955](Test-doc/1543639563955.png)

然后再发送一个120M的PDF文件

```bash
$ java -jar .\lftp_main.jar lsend -s 127.0.0.1:3000 ./data/book.pdf
```

![1543639727811](Test-doc/1543639727811.png)

然后测试并行发送

开启两个客户端，由于是并行发送，因此需要分别设置不同的不同的控制和发送端口，可以看到，服务器分别为这两个客户端开启了`20480`和`20482`端口，同时接收两者发送的数据。

测试1（发送过程）：

![1543641153710](Test-doc/1543641153710.png)

测试2（发送结束）：

![1543647401471](Test-doc/1543647401471.png)

查看其占用的内存，并不是把整个文件直接读入内存的

![1543639684093](Test-doc/1543639684093.png)

服务端信息：

接收到了两个文件，都是在`20480`端口完成传输的。

![1543639913941](Test-doc/1543639913941.png)



#### 查看文件列表

```bash
$ java -jar .\lftp_main.jar list -s 127.0.0.1:3000
```

可以看出文件被发送到服务端

![1543639815136](Test-doc/1543639815136.png)

#### 接收文件

![1543641317749](Test-doc/1543641317749.png)

```bash
$ java -jar .\lftp_main.jar lget -s 127.0.0.1:3000 -d ./download music.mp3
```

从服务端获取刚才发送的MP3文件，并放入`./download`文件夹中

![get](Test-doc/get.gif)

```bash
$ java -jar .\lftp_main.jar lget -s 127.0.0.1:3000 -d ./download book.pdf
```

然后下载刚才发送的比较大的120MB的pdf文件

![1543640732205](Test-doc/1543640732205.png)

服务端显示发送了三个文件，同样是通过`20480`端口发送的

![1543640594088](Test-doc/1543640594088.png)

现在再来测试以下并行接收：

从两个端口从服务端分别下载文件

```bash
$ java -jar .\lftp_main.jar lget -s 127.0.0.1:3000 -p 5555 -d ./download book.pdf
$ java -jar .\lftp_main.jar lget -s 127.0.0.1:3000 -p 5556 -d ./download music.mp3
```

![1543647554621](Test-doc/1543647554621.png)

两个客户端同时接收来自服务器的数据，分别在不同的端口。

服务端也同时开启了两个端口发生数据给客户端：

![1543647622606](Test-doc/1543647622606.png)

最后两个客户端都成功完整接收到文件

![1543647691480](Test-doc/1543647691480.png)



## 互联网测试

### 测试环境

服务器：`腾讯云`

配置：`2核2G`

带宽：上行`1Mbps`，下行`8Mbps`

系统：`CentOS 7.3 64位`

运行环境：`Java 11`

公网IP：`111.230.240.70 `(广州)

指向服务器IP的域名：`love.zhenly.cn`

由于比较穷，只能在1Mbps的小水管服务器上测试

### 服务端

指定在`3001`端口上开启服务

```bash
$ java -jar lftp_main.jar server -p 3001
```

![1543641837032](Test-doc/1543641837032.png)



### 客户端

#### 发送文件

这里通过url向服务器发送一个mp3文件

```bash
$ java -jar lftp_main.jar lsend -s love.zhenly.cn:3001 ./data/music.mp3
```

![1543641960485](Test-doc/1543641960485.png)

![1543642612975](Test-doc/1543642612975.png)

整个文件都被完整发送到服务器上

再发送一个120MB的pdf文件

```bash
$ java -jar .\lftp_main.jar lsend -s love.zhenly.cn:3001 ./data/book.pdf
```

![1543642688224](Test-doc/1543642688224.png)



服务端显示发送了若干个文件到客户端中

![1543642758877](Test-doc/1543642758877.png)

测试并行发送

需要指定不同控制和数据端口，否则端口绑定会发生冲突

```bash
$ java -jar .\lftp_main.jar lsend -s love.zhenly.cn:3001 -c 6666 -p 6667 ./data/book.pdf
$ java -jar .\lftp_main.jar lsend -s love.zhenly.cn:3001 ./data/music.mp3
```



![1543643368409](Test-doc/1543643368409.png)

从服务端接收日志可以看出，两个文件分别再两个不同的端口进行接收

![1543643488643](Test-doc/1543643488643.png)

#### 查看文件列表

```bash
 java -jar .\lftp_main.jar list -s love.zhenly.cn:3001
```

![1543642808535](Test-doc/1543642808535.png)

可以看到我们刚才发送到服务端的几个文件



#### 接收文件

由于这台服务器的上行带宽只有`1Mbps`，因此最大带宽也就是`128KB/s`，这里一般都能跑到`100KB/s`左右。

先下载一个4M的mp3文件

```bash
$ java -jar .\lftp_main.jar lget -s love.zhenly.cn:3001 -d ./download music.mp3
```

![1543655113329](Test-doc/1543655113329.png)



再下载一个120M的pdf文件

```bash
$ java -jar .\lftp_main.jar lget -s love.zhenly.cn:3001 -d ./download book.pdf
```

![1543655150378](Test-doc/1543655150378.png)

可以看出速度的确很慢

然后测试并行接收

需要指定两个不同的端口来接收文件，否则端口会产生冲突

```bash
$ java -jar .\lftp_main.jar lget -s love.zhenly.cn:3001 -p 3456 -d ./download music.mp3
$ java -jar .\lftp_main.jar lget -s love.zhenly.cn:3001 -p 5678 -d ./download book.pdf
```

![1543655562740](Test-doc/1543655562740.png)

可以看到，两个并行的客户端同时下载可以占满`1M`的小水管

