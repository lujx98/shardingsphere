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

package org.apache.shardingsphere.mode.metadata.refresher.metadata.pushdown.type.view;

import org.apache.shardingsphere.infra.config.props.ConfigurationProperties;
import org.apache.shardingsphere.infra.database.core.type.DatabaseType;
import org.apache.shardingsphere.infra.metadata.database.ShardingSphereDatabase;
import org.apache.shardingsphere.infra.metadata.database.schema.pojo.AlterSchemaMetaDataPOJO;
import org.apache.shardingsphere.mode.metadata.refresher.metadata.pushdown.PushDownMetaDataRefresher;
import org.apache.shardingsphere.mode.persist.service.MetaDataManagerPersistService;
import org.apache.shardingsphere.sql.parser.statement.core.statement.ddl.DropViewStatement;

import java.util.Collection;

/**
 * Drop view push down meta data refresher.
 */
public final class DropViewPushDownMetaDataRefresher implements PushDownMetaDataRefresher<DropViewStatement> {
    
    @Override
    public void refresh(final MetaDataManagerPersistService metaDataManagerPersistService, final ShardingSphereDatabase database, final Collection<String> logicDataSourceNames,
                        final String schemaName, final DatabaseType databaseType, final DropViewStatement sqlStatement, final ConfigurationProperties props) {
        AlterSchemaMetaDataPOJO alterSchemaMetaDataPOJO = new AlterSchemaMetaDataPOJO(database.getName(), schemaName);
        sqlStatement.getViews().forEach(each -> {
            String viewName = each.getTableName().getIdentifier().getValue();
            alterSchemaMetaDataPOJO.getDroppedTables().add(viewName);
            alterSchemaMetaDataPOJO.getDroppedViews().add(viewName);
        });
        metaDataManagerPersistService.alterSchema(alterSchemaMetaDataPOJO);
    }
    
    @Override
    public Class<DropViewStatement> getType() {
        return DropViewStatement.class;
    }
}
