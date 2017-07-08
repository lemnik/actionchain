# ActionChain

ActionChain makes bouncing between the main thread and background threads much:

* Easier
* Cleaner
* Safer
* More easily tested

and allows for much more code-reuse. It may help you in other ways, in which case: please share!

ActionCommand with lambda's can be used exactly like this:
```
onBackground((name) -> "Hello <b>" + name + "</b>")
.then(onBackground((msg) -> Html.fromHtml(msg)))
.then(onForeground((msg) -> {
    Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
}))
.exec("Jason");
```

You can also capture an Action chain to be executed later:

```
private final ActionCommand.Chain<User, User> saveUser =
    new SaveLocalUserCommand()
    .then(new UploadUserCommand())
    .then(onForeground((user) -> {
        setUser(user);
    });
```

# Thread-hopping Chain-of-Command

Action chains follow something like the chain-of-commands pattern (also known as chain-of-responsibility).
In this case however:

* Aach command (`ActionCommand`) has a function that runs on a "background" thread (typically stolen from `AsyncTask` under the hood).
* The background function takes an input open (optionally) and produces an output object
* The output object is handed to a foreground consumer that runs on the main thread
* The output object is passed as the input for the next `ActionCommand` in the chain

Exceptions cause the entire chain to go into an error state, which by-default is a "pass forward" behaviour
Errors are always handled on the *main* thread, so it's something you can simply attach to the end of your chain:

```
onBackground((name) -> "Hello <b>" + name + "</b>")
.then(onBackground((msg) -> Html.fromHtml(msg)))
.then(onForeground((msg) -> {
    Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
}))
.then(onError(exception) -> {
    handleError(exception);
});
```

# Usage Patterns

In case you don't have Java 8 (or lambdas of some other form) enabled for your Android project,
Action chains can be implemented as `ActionCommand` classes instead (which is how they work
under-the-hood anyway). These are also useful when you just want to encapsulate some logic
in a way this easy to reuse, unit test, etc.

```
public class SaveUserCommand extends ActionCommand<User, User> {
    private final Database database;

    public SaveUserCommand(final Database database) {
        this.database = database;
    }

    public User save(final User user) {
        final long databaseId = database.save(user);
        return user.builder()
                   .setId(databaseId)
                   .build();
    }
}
```

These can then be used as:

```
public void onClick(View saveAndClose) {
    new SaveUserCommand(database)
        .then(SendUserToServer.INSTANCE)
        .then(new FinishActivity(this))
        .exec(userEditor.build());
}
```

# Building

This project is still pre-release, and building it is either a command-line `gradlew assemble` effort,
or will require Android Studio 3.0.
