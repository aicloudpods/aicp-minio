package com.aicloudpods;

import org.apache.commons.io.FileUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FileUtil;
import org.apache.hadoop.fs.LocalFileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.spark.SparkConf;
import org.apache.spark.SparkContext;
import org.apache.spark.sql.DataFrameReader;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.*;

import java.net.URI;

public class SparkMinIO {


    private static final StructType customerSchema = DataTypes.createStructType(new StructField[] {
            DataTypes.createStructField("id",  DataTypes.StringType, true),
            DataTypes.createStructField("name", DataTypes.StringType, true),
            DataTypes.createStructField("accno", DataTypes.StringType, true)
    });
    private static final StructType txnSchema = DataTypes.createStructType(new StructField[] {
            DataTypes.createStructField("id",  DataTypes.StringType, true),
           DataTypes.createStructField("mon", DataTypes.StringType, true),
            DataTypes.createStructField("accno", DataTypes.StringType, true),
            DataTypes.createStructField("amount", DataTypes.DoubleType, true)
    });

    enum Env{
        local("http://localhost:9000"),
        cluster("http://minio.minio.svc.cluster.local:9000");
        private String url;

        Env(String url) {
            this.url = url;
        }

        public String getUrl() {
            return url;
        }
    }

    enum Type{
        name,
        nameaccno,
        namemonth
    }
    public static void main(String args[]) throws Exception{
        Env env = Env.valueOf(args[0]);

        SparkConf conf = getSparkConfig(env);
        SparkContext sparkContext = new SparkContext(conf);
        sparkContext.setLogLevel("WARN");
        SparkSession spark = SparkSession.builder().sparkContext(sparkContext).getOrCreate();
        Configuration configuration = sparkContext.hadoopConfiguration();
        configuration.set("fs.s3a.endpoint", env.url);
        configuration.set("fs.s3a.access.key", "AKIAIOSFODNN7EXAMPLE");
        configuration.set("fs.s3a.secret.key", "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY");
        configuration.set("fs.s3a.path.style.access", "true");
        configuration.set("fs.s3a.impl", "org.apache.hadoop.fs.s3a.S3AFileSystem");
//        LocalFileSystem localFileSystem = FileSystem.getLocal(new Configuration());

        FileSystem fileSystem = FileSystem.get(new URI("s3a://aicp-minio/"),configuration);
        Dataset<Row> customers = spark.read()
                .format("minioSelectCSV")
                .schema(customerSchema)
                .load("s3://aicp-minio/customer.csv");

        Dataset<Row> txn = spark.read()
                .format("minioSelectCSV")
                .schema(txnSchema).load("s3://aicp-minio/txn.csv");
       // customers.show();

        //txn.show();

        Dataset <Row> joined = customers
                .alias("c")
                .join(txn.alias("t"),customers.col("accno").equalTo(txn.col("accno")));

        switch (Type.valueOf(args[1])){

            case name:{
                joined
                        .groupBy("name")
                        .sum("amount")
                        .orderBy("name")
                        .write()
                        .csv("name.csv");

                FileUtil.copyMerge(fileSystem,new Path("name.csv"),fileSystem,new Path("name1.csv"),true, configuration, null);
                break;
            }
            case nameaccno:{
                joined
                        .groupBy("name","c.accno")
                        .sum("amount")
                        .orderBy("name")
                        .write()
                        .csv("nameaccno.csv");
                break;
            }
            case namemonth:{
                joined
                        .groupBy("name","mon")
                        .sum("amount")
                        .orderBy("name","mon")
                        .write()
                .csv("namemonth.csv");
            }
        }

        spark.stop();
        sparkContext.stop();
    }

    private static SparkConf getSparkConfig(Env env) {
        SparkConf sparkConf = new SparkConf().setAppName("SparkMinIO");
        if(Env.local.equals(env)){
            sparkConf.setMaster("local[1]");
        }
        return sparkConf;
    }
}
