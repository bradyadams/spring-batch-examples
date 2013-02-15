/**
 * Copyright 2011 Michael R. Lange <michael.r.lange@langmi.de>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.langmi.spring.batch.examples.complex.jdbc.generic.export;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.junit.After;
import javax.sql.DataSource;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.test.AbstractJobTests;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * JobConfigurationTest.
 *
 * @author Michael R. Lange <michael.r.lange@langmi.de> 
 */
@ContextConfiguration(locations = {
    "classpath*:spring/batch/job/complex/jdbc/jdbc-generic-export-to-database-job.xml",
    "classpath*:spring/batch/setup/**/*.xml"})
@RunWith(SpringJUnit4ClassRunner.class)
public class JdbcGenericExportToDatabaseJobConfigurationTest extends AbstractJobTests{

    @Autowired
    private DataSource dataSource;
    private JdbcTemplate jdbcTemplate;
    /** CREATE statement for BUSINESS_OBJECTS table. */
    private static final String CREATE_ROOT_TABLE_SQL = "CREATE TABLE ROOT (ID INTEGER GENERATED BY DEFAULT AS IDENTITY, NAME VARCHAR (100))";
    private static final String CREATE_TARGET_TABLE_SQL = "CREATE TABLE TARGET (ID INTEGER, NAME VARCHAR (100))";
    private static final String INSERT = "INSERT INTO ROOT (NAME) VALUES (?)";
    private static final String DELETE_ROOT_TABLE_SQL = "DROP TABLE ROOT";
    private static final String DELETE_TARGET_TABLE_SQL = "DROP TABLE TARGET";
    private static final String SHUTDOWN_HSQLDB = "SHUTDOWN";
    private static final String COUNT_SQL = "SELECT COUNT(*) FROM TARGET";
    private static final int EXPECTED_COUNT = 40;

    /** Launch Test. */
    @Test
    public void testJob() throws Exception {
        // launch the job
        JobExecution jobExecution = this.launchJob(new JobParameters());

        // assert job run status
        assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());

        // assert read/written items
        for (StepExecution stepExecution : jobExecution.getStepExecutions()) {
            assertEquals(EXPECTED_COUNT, stepExecution.getReadCount());
            assertEquals(EXPECTED_COUNT, stepExecution.getWriteCount());
        }

        // assert items are written successfully to target table
        assertEquals(EXPECTED_COUNT, jdbcTemplate.queryForInt(COUNT_SQL));
    }

    /**
     * Setup the test with some test data.
     *
     * @throws Exception 
     */
    @Before
    public void before() throws Exception {
        // provide jdbc template for setup and later assertions
        jdbcTemplate = new JdbcTemplate(dataSource);
        // setup root table
        jdbcTemplate.execute(CREATE_ROOT_TABLE_SQL);
        // setup target table
        jdbcTemplate.execute(CREATE_TARGET_TABLE_SQL);
        // insert test data into root table
        jdbcTemplate.batchUpdate(INSERT, new BatchPreparedStatementSetter() {

            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ps.setString(1, String.valueOf(i));
            }

            @Override
            public int getBatchSize() {
                return EXPECTED_COUNT;
            }
        });
    }

    /**
     * Teardown the test by deleting the test data.
     *
     * @throws Exception 
     */
    @After
    public void after() throws Exception {
        // clear tables
        jdbcTemplate.execute(DELETE_ROOT_TABLE_SQL);
        jdbcTemplate.execute(DELETE_TARGET_TABLE_SQL);
        // shutdown HSQLDB properly
        jdbcTemplate.execute(SHUTDOWN_HSQLDB);
    }
}
