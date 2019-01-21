package com.z.graphql

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.readValue
import io.leangen.graphql.annotations.GraphQLArgument
import io.leangen.graphql.annotations.GraphQLContext
import io.leangen.graphql.annotations.GraphQLMutation
import io.leangen.graphql.annotations.GraphQLQuery
import io.leangen.graphql.spqr.spring.annotation.GraphQLApi
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.repository.CrudRepository
import org.springframework.http.HttpMethod
import org.springframework.security.access.annotation.Secured
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.util.*
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id


@SpringBootApplication
@EnableGlobalMethodSecurity(securedEnabled = true)
class GraphqlApplication(private val carRepository: CarRepository):ApplicationRunner, WebSecurityConfigurerAdapter() {
	override fun run(args: ApplicationArguments?) {
		carRepository.saveAll(
				listOf("Ferrari", "Jaguar", "Porsche", "Lamborghini", "Bugatti", "AMC Gremlin", "Triumph Stag", "Ford Pinto", "Yugo GV")
						.map { Car(name = it) }
		)
	}

	override fun configure(auth: AuthenticationManagerBuilder) {
		auth.inMemoryAuthentication().apply {
			withUser ("admin").password("{noop}admin").roles("USER","ADMIN")
			withUser("user").password("{noop}user").roles("USER")
		}
	}

	override fun configure(http: HttpSecurity) {
		http.authorizeRequests()
				.antMatchers(HttpMethod.POST,"/graphql").authenticated().and()
				.httpBasic().and()
				.csrf().disable()
	}
}

fun main(args: Array<String>) {
	runApplication<GraphqlApplication>(*args)
}

@GraphQLApi
@Service
class CarService(private val carRepository: CarRepository, private  val giphyService: GiphyService){
	@GraphQLQuery
	fun findAll() = carRepository.findAll().toList()

	@GraphQLQuery
	fun findById(@GraphQLArgument(name = "id") id:Long) = carRepository.findById(id)

	@GraphQLMutation
	fun save(@GraphQLArgument(name = "car") car:Car) = carRepository.save(car)

	@GraphQLQuery
	fun delete(@GraphQLArgument(name = "id") id:Long) = carRepository.deleteById(id)

	@Secured("ROLE_ADMIN")
	@GraphQLQuery(name = "gif")
	fun getGif(@GraphQLContext car: Car) = giphyService.getUrl(car.name)

	@GraphQLQuery(name = "random")
	fun random(@GraphQLContext car: Car) = (0..9999).random()
}

@Service
class GiphyService(private val objectMapper: ObjectMapper){
	@Value("\${giphy.key}")
	private lateinit var key:String
	var restTemplate = RestTemplate()

	fun getUrl(query:String) = objectMapper.readValue<ObjectNode>(
			restTemplate.getForObject("http://api.giphy.com/v1/gifs/search?q=$query&api_key=$key",String::class.java) ?: "")
			.apply { println(objectMapper.writeValueAsString(this)) }
			.get("data")?.get(0)?.get("url")?.asText() ?: ""
}

interface CarRepository:CrudRepository<Car,Long>

@Entity
data class Car(
		@Id @GeneratedValue val id:Long? = null,
		val name:String
)

fun IntRange.random() = Random().nextInt((endInclusive + 1) - start) +  start

