# Macaron Java back door #

The back door can detect the environment and use different techniques
to inject a hook in the HTTP request work flow. Then, with the presence of
the key `M4c4r0n` in any field, an agent is presented.

  * [History](historyAgent.md): Memorize last 20 requests
  * [JNDI](jndiAgent.md): Consult JNDI
  * [JMX](jmxAgent.md): Consult and invoke JMX
  * [JDBC](jdbcAgent.md): Invoke database
  * [Java/Javascript](javaAgent.md): Compile and execute code in the JavaEE server
  * [Shell](shellAgent.md): Invoke a shell