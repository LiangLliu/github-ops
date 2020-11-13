# github ops小工具

* 问题：
    * 向master提交代码后，Jenkins会触发build最终生成一个docker image
    * Argo CD 是通过指定docker image 的 tag 的方式，来部署对应工程版本
    * 此时我们需要将master分支构建后的image tag 更新到 Argo CD的repo中
    
* 解决：
    * 在Jenkins中设置定时任务，build完成后，更新Argo CD repo 的step
    * 通过调用 github api的方式操作，更新Argo CD 的 repo


## 构建jar包

* 构建jar包
```shell script
./gradlew clean && ./gradlew fatJar
```

* 运行
```shell script
java -jar build/libs/git-ops-fatJar-0.0.1-SNAPSHOT.jar 
```  


## 配置参数
* 默认路径为：gitops ： 

| 属性名              |    示例    |  说明  |
|:-------------------|:---------------:|:------|
| userAndRepo        | user/repo           | 访问的github地址              |
| token              | xxxxxxxxxxx      | 访问github的令牌                 |
| filePath            | 配置文件所在的粒径   | 临时文件缓存目录，用于保存临时处理的文件，处理后会被删除 |
| replaceMap         | <key,value>       | 需要替换的内容                                      |

## 运行
```shell script
java -jar build/libs/git-ops-fatJar-0.0.1-SNAPSHOT.jar user/repo token filepath [image.tag=11111, replicaCount=5]
```


## docker 打包


* 构建生成jar包并运行
```shell script
./gradlew clean && ./gradlew fatJar -x test && java -jar git-ops-fatJar-0.0.1-SNAPSHOT.jar

```

* 生成 docker image
```docker
docker build -t localhost/github-ops .
```


* 启动docker image
```docker
docker run --name github_ops -p 8080:8080 -t localhost/github-ops user/repo token filepath \[image.tag=11111,replicaCount=5\] 
```

* 进入docker container
```docker
docker exec -it github_ops sh
``` 

