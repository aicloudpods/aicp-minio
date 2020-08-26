package com.aicloudpods;

import com.mongodb.spark.MongoSpark;
import com.mongodb.spark.rdd.api.java.JavaMongoRDD;
import org.apache.hadoop.conf.Configuration;
import org.apache.spark.SparkConf;
import org.apache.spark.SparkContext;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;
import org.bson.Document;
import scala.Function1;

import java.util.Arrays;

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
        local("http://localhost:9000","127.0.0.1"),
        cluster("http://minio.minio.svc.cluster.local:9000","mongodb.mongodb.svc.cluster.local");
        private String url;
        private String mongoUrl;

        Env(String url,String mongoUrl) {
            this.url = url;
            this.mongoUrl = mongoUrl;
        }

        public String getUrl() {
            return url;
        }

        public String getMongoUrl() {
            return mongoUrl;
        }
    }

    enum Type{
        name,
        nameaccno,
        namemonth
    }

    /*
    arg[0] = Env
    arg[1] = Type
     */
    public static void main(String args[]) throws Exception{
        Env env = Env.valueOf(args[0]);

        SparkConf conf = getSparkConfig(env);
        conf.set("spark.mongodb.output.uri", "mongodb://root:fO1UGfRWwO@"+env.mongoUrl+":27017/aicp."+args[1]+"?authSource=admin");
        SparkContext sparkContext = new SparkContext(conf);
        //sparkContext.setLogLevel("WARN");
        SparkSession spark = SparkSession
                .builder()
                .sparkContext(sparkContext)
                .getOrCreate();
        Configuration configuration = sparkContext.hadoopConfiguration();
        configuration.set("fs.s3a.endpoint", env.url);
        configuration.set("fs.s3a.access.key", "AKIAIOSFODNN7EXAMPLE");
        configuration.set("fs.s3a.secret.key", "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY");
        configuration.set("fs.s3a.path.style.access", "true");
        configuration.set("fs.s3a.impl", "org.apache.hadoop.fs.s3a.S3AFileSystem");

        Dataset<Row> customers = spark.read()
                .format("minioSelectCSV")
                .schema(customerSchema)
                .load("s3://aicp-minio/customer.csv");

        Dataset<Row> txn = spark.read()
                .format("minioSelectCSV")
                .schema(txnSchema).load("s3://aicp-minio/txn.csv");

        Dataset <Row> joined = customers
                .alias("c")
                .join(txn.alias("t"),customers.col("accno").equalTo(txn.col("accno")));

        switch (Type.valueOf(args[1])){

            case name:{
                joined
                        .groupBy("name")
                        .sum("amount")
                        .orderBy("name");
                break;
            }
            case nameaccno:{
                joined
                        .groupBy("name","c.accno")
                        .sum("amount")
                        .orderBy("name");
                break;
            }
            case namemonth:{
                joined
                        .groupBy("name","mon")
                        .sum("amount")
                        .orderBy("name","mon");
            }
        }

        JavaSparkContext jsc = new JavaSparkContext(sparkContext);

        JavaRDD<Document> documents = joined.toJSON().toJavaRDD().map(new Function<String, Document>() {
            public Document call(final String json) throws Exception {
                return Document.parse(json);
            }
        });

        MongoSpark.save(documents);

        JavaRDD<Document> fromMongo = MongoSpark.load(jsc);
        ((JavaMongoRDD<Document>) fromMongo).toDF().show(1000);

        spark.stop();
        sparkContext.stop();
    }

    private static SparkConf getSparkConfig(Env env) {
        SparkConf sparkConf = new SparkConf()
                .setAppName("SparkMinIO");
        if(Env.local.equals(env)){
            sparkConf.setMaster("local[1]");
        }
        return sparkConf;
    }
}
