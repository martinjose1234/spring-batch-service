package com.batch.springbatchservice.batchprocessing;

import javax.sql.DataSource;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.PlatformTransactionManager;

import com.batch.springbatchservice.model.Person;

@Configuration
public class BatchConfiguration {
	
	@Autowired
	DataSource dataSource;
	
	// NOTE : IF U GET "UnsupportedClassVersionError", THIS PROJECT WILL ONLY RUN IN JAVA 17. (no need to change any java version in pom.xml, let it be 11. just change eclipse java into 17)
	
	// NOTE : IN THIS EG WE READING DATA FROM CSV FILE AND PROCESSING AND WRITING RESULT INTO DB (CSV -> DB). FOR DB -> CSV PLEASE REFER WORK TIPS->SPTING BATCH FOLDER. 

	// READER >>>>>>>>>>>>>>>>>>>
	@Bean
	public FlatFileItemReader<Person> reader() {
		return new FlatFileItemReaderBuilder<Person>().name("personItemReader")
				.resource(new ClassPathResource("sample-data.csv")).delimited()
				.names(new String[] { "firstName", "lastName" })
				.fieldSetMapper(new BeanWrapperFieldSetMapper<Person>() {
					{
						setTargetType(Person.class);
					}
				}).build();
	}

	// PROCESSOR >>>>>>>>>>>>>>>>
	@Bean
	public PersonItemProcessor processor() {
		return new PersonItemProcessor(); // <<<<<<< ITEM PROCESSOR
	}

	// WRITER >>>>>>>>>>>>>>>>>>>
	@Bean
	public JdbcBatchItemWriter<Person> writer() {
		return new JdbcBatchItemWriterBuilder<Person>()
				.itemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<>())
				.sql("INSERT INTO people (first_name, last_name) VALUES (:firstName, :lastName)").dataSource(dataSource)
				.build();
	}

	// STEP BUILDER >>>>>>>>>>>>>
	@Bean
	public Step step1(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
		return new StepBuilder("step1", jobRepository).<Person, Person>chunk(10, transactionManager)
				.reader(reader())
				.processor(processor())
				.writer(writer()).build();
	}

	// JOB BUILDER >>>>>>>>>>>>>> (Job Builder will automatically take step1 bean)
	@Bean
	public Job importUserJob(JobRepository jobRepository, JobCompletionNotificationListener listener, Step step) {
		return new JobBuilder("importUserJob", jobRepository).incrementer(new RunIdIncrementer()).listener(listener) // <<< JOB COMPLETION NOTIFICATION LISTENER (JobCompletionNotificationListener.java)
				.flow(step).end().build();
	}

}
