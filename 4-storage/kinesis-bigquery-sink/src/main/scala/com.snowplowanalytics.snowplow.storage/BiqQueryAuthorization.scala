 /*
 * Copyright (c) 2014 Snowplow Analytics Ltd.
 * All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache
 * License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at
 * http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied.
 *
 * See the Apache License Version 2.0 for the specific language
 * governing permissions and limitations there under.
 */

package com.snowplowanalytics.snowplow.storage.kinesis.bigquery

// Java
import java.io.{
  FileInputStream,
  IOException,
  InputStreamReader,
  PrintStream
}
import java.util.{
  Arrays,
  Collections,
  List,
  Scanner
}

// Scala
import collection.JavaConversions._

//Java Libraries
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.googleapis.auth.oauth2.{
  GoogleAuthorizationCodeFlow,
  GoogleAuthorizationCodeRequestUrl,
  GoogleClientSecrets,
  GoogleTokenResponse
}
import com.google.api.client.http.HttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.util.Data
import com.google.api.services.bigquery.{
  Bigquery,
  BigqueryScopes
}
import com.google.api.services.bigquery.model.{
  GetQueryResultsResponse,
  QueryRequest,
  QueryResponse,
  TableCell,
  Dataset,
  DatasetReference,
  TableRow
}

/**
 * Authenticates the user and makes a query to the wikipedia database
 */
object bigQueryAuth {

  /**
   * Obtained from the Google developers console.
   */
  val ProjectId = "742196692985"

  /**
   * Location of the client secrets. This file is obtained from
   * the Google developers console.
   */
  val ClientSecretsLocation = "client_secrets.json"
  val HttpTransport = new NetHttpTransport
  val JsonFactory = new JacksonFactory

  /**
   * Handles OAuth authorization and makes a single query to the public 
   * wikipedia sample database
   */
  //def main(args: Array[String]) {
   
    //val credentials = {
      //val fis = new FileInputStream(ClientSecretsLocation)
      //val reader = new InputStreamReader(fis)
      //val gcs = GoogleClientSecrets.load(new JacksonFactory, reader)
      //getCredentials(gcs)
    //}
	 
    //val bigquery = new Bigquery(HttpTransport, JsonFactory, credentials)
    //val query = "SELECT TOP( title, 10) as title, COUNT(*) as revision_count " +
      //"FROM [publicdata:samples.wikipedia] WHERE wp_namespace = 0;"
    //runQueryRpcAndPrint(bigquery, ProjectId, query, System.out);
  //}
  
  //TODO
  //main should:  authenticate to bigquerytest project (project_id: ac-bigquery-tests, project_no: 742196692985) x
  //              create dataset x
  //              create table
  //              populate table 
  //              query table
  //              add row to table
  //              remove row from table 
  //              delete table

  def main(args: Array[String]){

    val credentials = {
      val fis = new FileInputStream(ClientSecretsLocation)
      val reader = new InputStreamReader(fis)
      val gcs = GoogleClientSecrets.load(new JacksonFactory, reader)
      getCredentials(gcs)
    }
    
    val bigquery = new Bigquery(HttpTransport, JsonFactory, credentials)
    
    createDataSet(ProjectId, bigquery)
  }

  /**
   * Creates a dataset with name testdataset.
   */
  def createDataSet (projectId: String, bigquery: Bigquery) = {
    val dataSetId = "testdataset"
    val dataSet = new Dataset
    val dataSetReference = new DatasetReference
    dataSetReference.setProjectId(projectId)
    dataSetReference.setDatasetId(dataSetId)
    dataSet.setDatasetReference(dataSetReference)
    try{
      bigquery.datasets.insert(projectId, dataSet).execute()
      println("Dataset created")
    }catch{
      case ex: IOException =>
        println("There's been an IOException: " + ex)
    }
  }
    




  /**
   * Prompts the user to visit the google API authorization page. The user 
   * can then grant access to the API, and if so is given an access code. The 
   * user is prompted to paste this code in to the command line. The code grants
   * the applicatin access to the database.
   *
   * @param clientSecrets
   *
   * @return valid credentials
   */
  def getCredentials(clientSecrets: GoogleClientSecrets): Credential = {

    val scopes = Collections.singleton(BigqueryScopes.BIGQUERY)
    val authorizeUrl = new GoogleAuthorizationCodeRequestUrl(clientSecrets, clientSecrets.getInstalled().getRedirectUris().get(0), scopes).build()
    println("Paste this URL into a web browser to authorize BigQuery Access:\n" + authorizeUrl)
    println("... and paste the code you received here: ")
    val authorizationCode = readLine()

    // Exchange the auth code for an access token
    val flow = new GoogleAuthorizationCodeFlow.Builder(HttpTransport, JsonFactory, clientSecrets, Arrays.asList(BigqueryScopes.BIGQUERY)).build()
    val response = flow.newTokenRequest(authorizationCode).setRedirectUri(clientSecrets.getInstalled.getRedirectUris.get(0)).execute();
    flow.createAndStoreCredential(response,null)
  }

  /**
   * Makes a request to the database and displays the response.
   *
   * @param bigquery
   * @param projectId
   * @param query [[https://cloud.google.com/bigquery/query-reference bigquery database query]]
   * @param out
   */
  def runQueryRpcAndPrint(bigquery: Bigquery, projectId: String, query: String, out: PrintStream) {
    val queryRequest = new QueryRequest().setQuery(query)
    val queryResponse = bigquery.jobs.query(projectId, queryRequest).execute()
    
    if (queryResponse.getJobComplete) {
      printRows(queryResponse.getRows, out)
      if (Option(queryResponse.getPageToken).isDefined) {
        return
      }
    }

    while(true) {
      var pageToken: String = null

      val queryResults = bigquery.jobs
        .getQueryResults(projectId, queryResponse.getJobReference.getJobId)
        .setPageToken(pageToken).execute()

      if (queryResults.getJobComplete) {
        printRows(queryResponse.getRows, out)
        pageToken = queryResults.getPageToken

        if (Option(pageToken).isDefined) {
          return
        }
      }
    }
  }

  /**
   * Store the refresh token in the file 
   * snowplow_bigquery_refresh_token.properties
   */
  def storeRefreshToken(refreshToken: String){
    val properties = new Properties
    properties.setProperty("refreshtoken": refreshToken)
    try {
      properties.store(new FileOutputStream("snowplow_bigquery_refresh_token.properties"))
      println("Refresh token saved.")
    } catch {
        case ex: FileNotFoundException => 
          println("FileNotFoundException: " + ex)
        case ex: IOException => 
          println("IOException: " + ex)
    }
  }
  
  /**
   * Load the refresh token from the file 
   * snowplow_bigquery_refresh_token.properties
   */
  def loadRefreshToken: String = {
    val properties = new Properties
    try {
      properties.load(new FileInputStream("snowplow_bigquery_refresh_token.properties"))
      properties.get("refreshtoken");
    } catch {
        case ex: FileNotFoundException => 
          println("FileNotFoundException: " + ex)
        case ex: IOException => 
          println("IOException: " + ex)
    }
  }

  /**
   * Prints the query response to standars output.
   */
  def printRows(rows: List[TableRow], out: PrintStream){

    def outputCell(cell: TableCell) {
      val cellData = if (Data.isNull(cell.getV)) {
        "Null"
      } else {
        cell.getV.toString
      }
      out.printf("%s, ", cellData)
    }
    if (rows != null) {
      for (row <- rows) {
        for (cell <- row.getF) {
          outputCell(cell)
        }
        out.println
      }
    }
  }
  
}
