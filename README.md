# ActionChain

Turns ugly `AsyncTask` implementations into:
```
onBackground((name) -> {
    Thread.sleep(2000);
    return "Hello <b>" + name + "</b>";
})
.then(onBackground((msg) -> Html.fromHtml(msg)))
.then(onForeground((msg) -> {
    Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
}))
.exec("Jason");
```

Building this project may require Android Studio 3.0 or higher.

# Usage Patterns

In case you don't have Java 8 (or lambdas of some other form) enabled for your Android project,
`ActionChain`s can be implemented as `ActionCommand` classes instead. These are also useful when
you just want to encapsulate some logic in a way this easy to reuse, unit test, etc.

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
