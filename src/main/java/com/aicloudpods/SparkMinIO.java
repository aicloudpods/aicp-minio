package com.aicloudpods;

import org.apache.spark.SparkConf;
import org.apache.spark.SparkContext;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.*;

public class SparkMinIO {

    public static void main(String args[]){

        SparkConf conf = new SparkConf().setAppName("SparkMinIO");
        SparkContext sparkContext = new SparkContext(conf);
        sparkContext.setLogLevel("WARN");
        SparkSession spark = SparkSession.builder().sparkContext(sparkContext).getOrCreate();
        sparkContext.hadoopConfiguration().set("fs.s3a.endpoint", "http://minio.default.svc.cluster.local:9000");
        sparkContext.hadoopConfiguration().set("fs.s3a.access.key", "AKIAIOSFODNN7EXAMPLE");
        sparkContext.hadoopConfiguration().set("fs.s3a.secret.key", "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY");
        sparkContext.hadoopConfiguration().set("fs.s3a.path.style.access", "true");
        sparkContext.hadoopConfiguration().set("fs.s3a.impl", "org.apache.hadoop.fs.s3a.S3AFileSystem");

        StructType schema = DataTypes.createStructType(new StructField[] {
                DataTypes.createStructField("id",  DataTypes.StringType, true),
                DataTypes.createStructField("name", DataTypes.StringType, true)
        });


        Dataset<Row> minioSelectCSV = spark.read()
                .format("minioSelectCSV")
                .schema(schema)
                .load("s3://aicp-minio/customer.csv");

        minioSelectCSV.show();
        minioSelectCSV.printSchema();
    }
}
