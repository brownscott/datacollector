/**
 * Copyright 2016 StreamSets Inc.
 *
 * Licensed under the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.streamsets.pipeline.stage.origin.jdbc.table;

import com.google.common.base.Joiner;
import com.google.common.collect.Sets;
import com.streamsets.pipeline.api.StageException;
import com.streamsets.pipeline.lib.jdbc.JdbcErrors;
import com.streamsets.pipeline.lib.jdbc.JdbcUtil;
import org.apache.commons.lang3.StringUtils;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public final class TableContextUtil {
  //JDBC Result set constants
  private static final String TABLE_METADATA_TABLE_CATALOG_CONSTANT = "TABLE_CAT";
  private static final String TABLE_METADATA_TABLE_SCHEMA_CONSTANT = "TABLE_SCHEM";
  private static final String TABLE_METADATA_TABLE_NAME_CONSTANT = "TABLE_NAME";
  private static final String COLUMN_METADATA_COLUMN_NAME = "COLUMN_NAME";
  private static final String COLUMN_METADATA_COLUMN_TYPE = "DATA_TYPE";

  private static final Joiner COMMA_JOINER = Joiner.on(",");


  private TableContextUtil() {}

  private static Map<String, Integer> getColumnNameType(Connection connection, String tableName) throws SQLException {
    Map<String, Integer> columnNameToType = new LinkedHashMap<>();
    try (ResultSet rs = JdbcUtil.getColumnMetadata(connection, tableName)) {
      while (rs.next()) {
        String column = rs.getString(COLUMN_METADATA_COLUMN_NAME);
        int type = rs.getInt(COLUMN_METADATA_COLUMN_TYPE);
        columnNameToType.put(column, type);
      }
    }
    return columnNameToType;
  }

  /**
   * Returns qualified table name (schema.table name)
   * @param schema schema name, can be null
   * @param tableName table name
   * @return qualified table name if schema is not null and tableName alone if schema is null.
   */
  public static String getQualifiedTableName(String schema, String tableName) {
    return StringUtils.isEmpty(schema) ? tableName : schema + "." + tableName ;
  }

  private static TableContext createTableContext(
      Connection connection,
      String schemaName,
      String tableName,
      TableConfigBean tableConfigBean
  ) throws SQLException, StageException {
    LinkedHashMap<String, Integer> partitionColumnToType = new LinkedHashMap<>();
    //Even though we are using this only find partition column's type, we could cache it if need arises.
    Map<String, Integer> columnNameToType = getColumnNameType(connection, tableName);

    Map<String, String> partitionColumnToStartOffset = new HashMap<>();
    if (tableConfigBean.overridePartitionColumns) {
      for (String overridenPartitionColumn : tableConfigBean.partitionColumns) {
        if (!columnNameToType.containsKey(overridenPartitionColumn)) {
          throw new StageException(JdbcErrors.JDBC_63, tableName, overridenPartitionColumn);
        }
        partitionColumnToType.put(overridenPartitionColumn, columnNameToType.get(overridenPartitionColumn));
      }
    } else {
      List<String> primaryKeys = JdbcUtil.getPrimaryKeys(connection, tableName);
      if (primaryKeys.isEmpty()) {
        throw new StageException(JdbcErrors.JDBC_62, tableName);
      }
      for (String primaryKey : primaryKeys) {
        partitionColumnToType.put(primaryKey, columnNameToType.get(primaryKey));
      }
    }

    //Initial offset should exist for all partition columns or none at all.
    if (!tableConfigBean.offsetColumnToInitialOffsetValue.isEmpty()) {
      Set<String> missingColumns = Sets.difference(partitionColumnToType.keySet(), tableConfigBean.offsetColumnToInitialOffsetValue.keySet());
      Set<String> extraColumns = Sets.difference(tableConfigBean.offsetColumnToInitialOffsetValue.keySet(), partitionColumnToType.keySet());

      if (!missingColumns.isEmpty() || !extraColumns.isEmpty()) {
        throw new StageException(JdbcErrors.JDBC_64, COMMA_JOINER.join(missingColumns), COMMA_JOINER.join(extraColumns));
      }

      for (Map.Entry<String, String> partitionColumnInitialOffsetEntry : tableConfigBean.offsetColumnToInitialOffsetValue.entrySet()) {
        partitionColumnToStartOffset.put(partitionColumnInitialOffsetEntry.getKey(), partitionColumnInitialOffsetEntry.getValue());
      }
    }
    return new TableContext(
        schemaName,
        tableName,
        partitionColumnToType,
        partitionColumnToStartOffset
    );
  }

  /**
   * Lists all tables matching the {@link TableConfigBean} and creates a table context for each table.
   * @param connection JDBC connection
   * @param tableConfigBean {@link TableConfigBean}
   * @return Map of qualified table name to Table Context
   * @throws SQLException If list tables call fails
   * @throws StageException if partition configuration is not correct.
   */
  public static Map<String, TableContext> listTablesForConfig(
      Connection connection,
      TableConfigBean tableConfigBean
  ) throws SQLException, StageException {
    Map<String, TableContext> tableContextMap = new LinkedHashMap<>();
    Pattern p = (StringUtils.isEmpty(tableConfigBean.tableExclusionPattern))? null : Pattern.compile(tableConfigBean.tableExclusionPattern);
    try (ResultSet rs = JdbcUtil.getTableMetadata(connection, null, tableConfigBean.schema, tableConfigBean.tablePattern)) {
      while (rs.next()) {
        String schemaName = rs.getString(TABLE_METADATA_TABLE_SCHEMA_CONSTANT);
        String tableName = rs.getString(TABLE_METADATA_TABLE_NAME_CONSTANT);
        if (p == null || !p.matcher(tableName).matches()) {
          tableContextMap.put(
              getQualifiedTableName(schemaName, tableName),
              createTableContext(connection, schemaName, tableName, tableConfigBean)
          );
        }
      }
    }
    return tableContextMap;
  }

  /**
   * Determines whether the actualSqlType is one of the sqlTypes list
   * @param actualSqlType the actual sql type
   * @param sqlTypes arbitrary list of sql types
   * @return true if actual Sql Type is one of the sql Types else false.
   */
  public static boolean isSqlTypeOneOf(int actualSqlType, int... sqlTypes) {
    for (int sqlType : sqlTypes) {
      if (sqlType == actualSqlType) {
        return true;
      }
    }
    return false;
  }
}
