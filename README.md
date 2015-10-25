= Scopes

== Method Scope

Method scope provides a way to control the activation/deactivation of a scope
with an annotation. All CDI `Bean<?>` scoped with `@MethodScoped` in this context will use the same instance.

=== Activation of the scope:

==== Declaratively

```java
@WithScope
public void methodWithMethodScopedActivate() {
    // use method scoped beans
}
```

==== Programmatically

```java
@Inject
private MethodScopeExtension extension;

public void inScope() {
    extension.executeInScope(() -> {  /* do in scope */ });
}
```

=== Usage of the scope

Just use `@MethodScoped`:

````java
@MethodScoped
@Produces
public Foo foo() {
    // ...
}
````

=== Use case

Common use case for such a scope is to scope a resource and ensure thanks to CDI to release
resources.

If you think to a socket connection of a pooled resource this comes obvious, you can now use this pattern:


```java
@Produces
@MethodScoped
public MyScopedConnection connection() { /*...*/ }

public void release(@Disposes MyScopedConnection connection) {
    connection.close();
}
```

And in a service bean or JAXRS resource:

```java
@WithScope
public Foo useConnection() {
    // ...
}
```
