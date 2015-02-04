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

package com.snowplowanalytics.snowplow.storage.kinesis

// Scalaz
import scalaz._
import Scalaz._

// BigQuery
import com.google.api.services.bigquery.model.TableRow

package object bigquery{

  /**
   * Scala object representaion of a single row, so called as it is typically
   * created from a Kinesis Record and used to create a Bigquery TableRow.
   */
  type IntermediateRecord = List[(String, String, String)]

  /**
   * Alias for TableRow class from bigquery library. Objects of this type are the 
   * outputs and inputs of the transformer and transmitter respectively.
   */
  type BigQueryTableRow = TableRow
}

