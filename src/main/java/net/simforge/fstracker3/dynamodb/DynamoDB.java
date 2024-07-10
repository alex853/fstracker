package net.simforge.fstracker3.dynamodb;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import net.simforge.commons.legacy.misc.Settings;

public class DynamoDB {

    private static final String amazonDynamoDBEndpoint = Settings.get("amazon.dynamodb.endpoint");
    private static final String amazonAWSAccessKey = Settings.get("amazon.dynamodb.access.key");
    private static final String amazonAWSSecretKey = Settings.get("amazon.dynamodb.secret.key");

    private static AmazonDynamoDB client = null;

    public static synchronized AmazonDynamoDB get() {
        if (client == null) {
            client = amazonDynamoDB();
        }
        return client;
    }

    private static AmazonDynamoDB amazonDynamoDB() {
        AmazonDynamoDBClientBuilder amazonDynamoDBClientBuilder = AmazonDynamoDBClientBuilder
                .standard()
                .withCredentials(new AWSStaticCredentialsProvider(amazonAWSCredentials()));
        amazonDynamoDBClientBuilder.setEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(amazonDynamoDBEndpoint, null));
        return amazonDynamoDBClientBuilder.build();
    }

    private static AWSCredentials amazonAWSCredentials() {
        return new BasicAWSCredentials(
                amazonAWSAccessKey, amazonAWSSecretKey);
    }

    public static String getUserId() {
        return "afe78778-39d3-494d-b366-e696e75b96a4";
    }
}
