# RecorderMp4

注意：1、mMuxer!!.writeSampleData(videoTrack, encodedData, bufferInfo)
有可能写的时候会出现IllegalStateException，提示到这行的话，那应该是原本有一个文件，所以崩溃，这个得看不同的安卓板而定
2、要是运行崩溃，可以通过Encoder中下面的几个方法进行查看适合的硬编码，或者上网搜索查看硬编码的代码，不同安卓板支持的硬编码不一样

1、CameraActivity和Encoder、CameraHelper是可以做到录制视频，视频的质量可以达到30帧的，要是视频帧率不够可以联系安卓板厂家

其他的camerax是我在尝试的方向，还没有调试好，如果有兴趣可以尝试一下，如果单纯使用可以直接删除其他的类

RecordMp4一般是对录像的管理，可以参考一下把开关录像的逻辑写进去
