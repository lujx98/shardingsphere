/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shardingsphere.sharding.route.engine.type;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.shardingsphere.infra.binder.context.statement.SQLStatementContext;
import org.apache.shardingsphere.infra.binder.context.statement.ddl.CloseStatementContext;
import org.apache.shardingsphere.infra.binder.context.type.CursorAvailable;
import org.apache.shardingsphere.infra.binder.context.type.TableAvailable;
import org.apache.shardingsphere.infra.config.props.ConfigurationProperties;
import org.apache.shardingsphere.infra.hint.HintValueContext;
import org.apache.shardingsphere.infra.metadata.database.ShardingSphereDatabase;
import org.apache.shardingsphere.infra.session.connection.ConnectionContext;
import org.apache.shardingsphere.infra.session.query.QueryContext;
import org.apache.shardingsphere.sharding.route.engine.condition.ShardingCondition;
import org.apache.shardingsphere.sharding.route.engine.condition.ShardingConditions;
import org.apache.shardingsphere.sharding.route.engine.type.broadcast.ShardingDataSourceGroupBroadcastRoutingEngine;
import org.apache.shardingsphere.sharding.route.engine.type.broadcast.ShardingDatabaseBroadcastRoutingEngine;
import org.apache.shardingsphere.sharding.route.engine.type.broadcast.ShardingInstanceBroadcastRoutingEngine;
import org.apache.shardingsphere.sharding.route.engine.type.broadcast.ShardingTableBroadcastRoutingEngine;
import org.apache.shardingsphere.sharding.route.engine.type.complex.ShardingComplexRoutingEngine;
import org.apache.shardingsphere.sharding.route.engine.type.ignore.ShardingIgnoreRoutingEngine;
import org.apache.shardingsphere.sharding.route.engine.type.standard.ShardingStandardRoutingEngine;
import org.apache.shardingsphere.sharding.route.engine.type.unicast.ShardingUnicastRoutingEngine;
import org.apache.shardingsphere.sharding.rule.ShardingRule;
import org.apache.shardingsphere.sql.parser.statement.core.statement.SQLStatement;
import org.apache.shardingsphere.sql.parser.statement.core.statement.dal.AnalyzeTableStatement;
import org.apache.shardingsphere.sql.parser.statement.core.statement.dal.DALStatement;
import org.apache.shardingsphere.sql.parser.statement.core.statement.dal.LoadStatement;
import org.apache.shardingsphere.sql.parser.statement.core.statement.dal.ResetParameterStatement;
import org.apache.shardingsphere.sql.parser.statement.core.statement.dal.SetStatement;
import org.apache.shardingsphere.sql.parser.statement.core.statement.dcl.DCLStatement;
import org.apache.shardingsphere.sql.parser.statement.core.statement.ddl.AlterFunctionStatement;
import org.apache.shardingsphere.sql.parser.statement.core.statement.ddl.AlterProcedureStatement;
import org.apache.shardingsphere.sql.parser.statement.core.statement.ddl.AlterTablespaceStatement;
import org.apache.shardingsphere.sql.parser.statement.core.statement.ddl.CreateFunctionStatement;
import org.apache.shardingsphere.sql.parser.statement.core.statement.ddl.CreateProcedureStatement;
import org.apache.shardingsphere.sql.parser.statement.core.statement.ddl.CreateTablespaceStatement;
import org.apache.shardingsphere.sql.parser.statement.core.statement.ddl.DDLStatement;
import org.apache.shardingsphere.sql.parser.statement.core.statement.ddl.DropFunctionStatement;
import org.apache.shardingsphere.sql.parser.statement.core.statement.ddl.DropProcedureStatement;
import org.apache.shardingsphere.sql.parser.statement.core.statement.ddl.DropTablespaceStatement;
import org.apache.shardingsphere.sql.parser.statement.core.statement.dml.DMLStatement;
import org.apache.shardingsphere.sql.parser.statement.core.statement.tcl.TCLStatement;
import org.apache.shardingsphere.sql.parser.statement.mysql.dal.MySQLCreateResourceGroupStatement;
import org.apache.shardingsphere.sql.parser.statement.mysql.dal.MySQLOptimizeTableStatement;
import org.apache.shardingsphere.sql.parser.statement.mysql.dal.MySQLSetResourceGroupStatement;
import org.apache.shardingsphere.sql.parser.statement.mysql.dal.MySQLShowDatabasesStatement;
import org.apache.shardingsphere.sql.parser.statement.mysql.dal.MySQLUseStatement;

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

/**
 * Sharding routing engine factory.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ShardingRouteEngineFactory {
    
    /**
     * Create new instance of routing engine.
     *
     * @param shardingRule sharding rule
     * @param database database
     * @param queryContext query context
     * @param shardingConditions shardingConditions
     * @param props ShardingSphere properties
     * @param connectionContext connection context
     * @return created instance
     */
    public static ShardingRouteEngine newInstance(final ShardingRule shardingRule, final ShardingSphereDatabase database, final QueryContext queryContext,
                                                  final ShardingConditions shardingConditions, final ConfigurationProperties props, final ConnectionContext connectionContext) {
        SQLStatementContext sqlStatementContext = queryContext.getSqlStatementContext();
        SQLStatement sqlStatement = sqlStatementContext.getSqlStatement();
        if (sqlStatement instanceof TCLStatement) {
            return new ShardingDatabaseBroadcastRoutingEngine();
        }
        if (sqlStatement instanceof DDLStatement) {
            if (sqlStatementContext instanceof CursorAvailable) {
                return getCursorRouteEngine(shardingRule, database, sqlStatementContext, queryContext.getHintValueContext(), shardingConditions, props);
            }
            return getDDLRoutingEngine(shardingRule, database, sqlStatementContext);
        }
        if (sqlStatement instanceof DALStatement) {
            return getDALRoutingEngine(shardingRule, database, sqlStatementContext, connectionContext);
        }
        if (sqlStatement instanceof DCLStatement) {
            return getDCLRoutingEngine(shardingRule, database, sqlStatementContext);
        }
        return getDQLRoutingEngine(shardingRule, database, sqlStatementContext, queryContext.getHintValueContext(), shardingConditions, props, connectionContext);
    }
    
    private static ShardingRouteEngine getDDLRoutingEngine(final ShardingRule shardingRule, final ShardingSphereDatabase database, final SQLStatementContext sqlStatementContext) {
        SQLStatement sqlStatement = sqlStatementContext.getSqlStatement();
        boolean functionStatement = sqlStatement instanceof CreateFunctionStatement || sqlStatement instanceof AlterFunctionStatement || sqlStatement instanceof DropFunctionStatement;
        boolean procedureStatement = sqlStatement instanceof CreateProcedureStatement || sqlStatement instanceof AlterProcedureStatement || sqlStatement instanceof DropProcedureStatement;
        if (functionStatement || procedureStatement) {
            return new ShardingDatabaseBroadcastRoutingEngine();
        }
        if (sqlStatement instanceof CreateTablespaceStatement || sqlStatement instanceof AlterTablespaceStatement || sqlStatement instanceof DropTablespaceStatement) {
            return new ShardingInstanceBroadcastRoutingEngine(database.getResourceMetaData());
        }
        Collection<String> tableNames = sqlStatementContext instanceof TableAvailable
                ? ((TableAvailable) sqlStatementContext).getTablesContext().getSimpleTables().stream().map(each -> each.getTableName().getIdentifier().getValue()).collect(Collectors.toSet())
                : Collections.emptyList();
        Collection<String> shardingRuleTableNames = shardingRule.getShardingRuleTableNames(tableNames);
        if (!tableNames.isEmpty() && shardingRuleTableNames.isEmpty()) {
            return new ShardingIgnoreRoutingEngine();
        }
        return new ShardingTableBroadcastRoutingEngine(database, sqlStatementContext, shardingRuleTableNames);
    }
    
    private static ShardingRouteEngine getCursorRouteEngine(final ShardingRule shardingRule, final ShardingSphereDatabase database, final SQLStatementContext sqlStatementContext,
                                                            final HintValueContext hintValueContext, final ShardingConditions shardingConditions, final ConfigurationProperties props) {
        if (sqlStatementContext instanceof CloseStatementContext && ((CloseStatementContext) sqlStatementContext).getSqlStatement().isCloseAll()) {
            return new ShardingDatabaseBroadcastRoutingEngine();
        }
        Collection<String> tableNames = sqlStatementContext instanceof TableAvailable ? ((TableAvailable) sqlStatementContext).getTablesContext().getTableNames() : Collections.emptyList();
        Collection<String> logicTableNames = shardingRule.getShardingLogicTableNames(tableNames);
        boolean allBindingTables = logicTableNames.size() > 1 && shardingRule.isAllBindingTables(database, sqlStatementContext, logicTableNames);
        if (isShardingStandardQuery(shardingRule, logicTableNames, allBindingTables)) {
            return new ShardingStandardRoutingEngine(getLogicTableName(shardingConditions, logicTableNames), shardingConditions, sqlStatementContext, hintValueContext, props);
        }
        return new ShardingIgnoreRoutingEngine();
    }
    
    private static ShardingRouteEngine getDALRoutingEngine(final ShardingRule shardingRule, final ShardingSphereDatabase database, final SQLStatementContext sqlStatementContext,
                                                           final ConnectionContext connectionContext) {
        SQLStatement sqlStatement = sqlStatementContext.getSqlStatement();
        if (sqlStatement instanceof MySQLUseStatement) {
            return new ShardingIgnoreRoutingEngine();
        }
        if (sqlStatement instanceof SetStatement || sqlStatement instanceof ResetParameterStatement || sqlStatement instanceof MySQLShowDatabasesStatement || sqlStatement instanceof LoadStatement) {
            return new ShardingDatabaseBroadcastRoutingEngine();
        }
        if (isResourceGroupStatement(sqlStatement)) {
            return new ShardingInstanceBroadcastRoutingEngine(database.getResourceMetaData());
        }
        Collection<String> tableNames = sqlStatementContext instanceof TableAvailable ? ((TableAvailable) sqlStatementContext).getTablesContext().getTableNames() : Collections.emptyList();
        Collection<String> shardingRuleTableNames = shardingRule.getShardingRuleTableNames(tableNames);
        if (!tableNames.isEmpty() && shardingRuleTableNames.isEmpty()) {
            return new ShardingIgnoreRoutingEngine();
        }
        if (sqlStatement instanceof MySQLOptimizeTableStatement) {
            return new ShardingTableBroadcastRoutingEngine(database, sqlStatementContext, shardingRuleTableNames);
        }
        if (sqlStatement instanceof AnalyzeTableStatement) {
            return shardingRuleTableNames.isEmpty() ? new ShardingDatabaseBroadcastRoutingEngine()
                    : new ShardingTableBroadcastRoutingEngine(database, sqlStatementContext, shardingRuleTableNames);
        }
        if (!shardingRuleTableNames.isEmpty()) {
            return new ShardingUnicastRoutingEngine(sqlStatementContext, shardingRuleTableNames, connectionContext);
        }
        return new ShardingDataSourceGroupBroadcastRoutingEngine();
    }
    
    private static boolean isResourceGroupStatement(final SQLStatement sqlStatement) {
        // TODO add dropResourceGroupStatement, alterResourceGroupStatement
        return sqlStatement instanceof MySQLCreateResourceGroupStatement || sqlStatement instanceof MySQLSetResourceGroupStatement;
    }
    
    private static ShardingRouteEngine getDCLRoutingEngine(final ShardingRule shardingRule, final ShardingSphereDatabase database, final SQLStatementContext sqlStatementContext) {
        if (isDCLForSingleTable(sqlStatementContext)) {
            Collection<String> tableNames = sqlStatementContext instanceof TableAvailable ? ((TableAvailable) sqlStatementContext).getTablesContext().getTableNames() : Collections.emptyList();
            Collection<String> shardingRuleTableNames = shardingRule.getShardingRuleTableNames(tableNames);
            return shardingRuleTableNames.isEmpty() ? new ShardingIgnoreRoutingEngine() : new ShardingTableBroadcastRoutingEngine(database, sqlStatementContext, shardingRuleTableNames);
        }
        return new ShardingInstanceBroadcastRoutingEngine(database.getResourceMetaData());
    }
    
    private static boolean isDCLForSingleTable(final SQLStatementContext sqlStatementContext) {
        if (sqlStatementContext instanceof TableAvailable) {
            TableAvailable tableSegmentsAvailable = (TableAvailable) sqlStatementContext;
            return 1 == tableSegmentsAvailable.getTablesContext().getSimpleTables().size()
                    && !"*".equals(tableSegmentsAvailable.getTablesContext().getSimpleTables().iterator().next().getTableName().getIdentifier().getValue());
        }
        return false;
    }
    
    private static ShardingRouteEngine getDQLRoutingEngine(final ShardingRule shardingRule, final ShardingSphereDatabase database, final SQLStatementContext sqlStatementContext,
                                                           final HintValueContext hintValueContext, final ShardingConditions shardingConditions, final ConfigurationProperties props,
                                                           final ConnectionContext connectionContext) {
        Collection<String> tableNames = sqlStatementContext instanceof TableAvailable ? ((TableAvailable) sqlStatementContext).getTablesContext().getTableNames() : Collections.emptyList();
        if (sqlStatementContext.getSqlStatement() instanceof DMLStatement && shardingConditions.isAlwaysFalse() || tableNames.isEmpty()) {
            return new ShardingUnicastRoutingEngine(sqlStatementContext, tableNames, connectionContext);
        }
        Collection<String> shardingLogicTableNames = shardingRule.getShardingLogicTableNames(tableNames);
        if (shardingLogicTableNames.isEmpty()) {
            return new ShardingIgnoreRoutingEngine();
        }
        return getDQLRouteEngineForShardingTable(shardingRule, database, sqlStatementContext, hintValueContext, shardingConditions, props, shardingLogicTableNames);
    }
    
    private static ShardingRouteEngine getDQLRouteEngineForShardingTable(final ShardingRule shardingRule, final ShardingSphereDatabase database,
                                                                         final SQLStatementContext sqlStatementContext, final HintValueContext hintValueContext,
                                                                         final ShardingConditions shardingConditions, final ConfigurationProperties props, final Collection<String> tableNames) {
        boolean allBindingTables = tableNames.size() > 1 && shardingRule.isAllBindingTables(database, sqlStatementContext, tableNames);
        if (isShardingStandardQuery(shardingRule, tableNames, allBindingTables)) {
            return new ShardingStandardRoutingEngine(getLogicTableName(shardingConditions, tableNames), shardingConditions, sqlStatementContext, hintValueContext, props);
        }
        // TODO config for cartesian set
        return new ShardingComplexRoutingEngine(shardingConditions, sqlStatementContext, hintValueContext, props, tableNames);
    }
    
    private static String getLogicTableName(final ShardingConditions shardingConditions, final Collection<String> tableNames) {
        if (shardingConditions.getConditions().isEmpty()) {
            return tableNames.iterator().next();
        }
        ShardingCondition shardingCondition = shardingConditions.getConditions().iterator().next();
        return shardingCondition.getValues().isEmpty() ? tableNames.iterator().next() : shardingCondition.getValues().iterator().next().getTableName();
    }
    
    private static boolean isShardingStandardQuery(final ShardingRule shardingRule, final Collection<String> tableNames, final boolean allBindingTables) {
        return 1 == tableNames.size() && shardingRule.isAllShardingTables(tableNames) || allBindingTables;
    }
}
