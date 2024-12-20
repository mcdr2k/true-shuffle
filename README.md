# True Shuffle

Shuffles your Spotify playlist in a truly random fashion.

## Installation

This project makes use of the maven build tool. Run `mvn package` to create a jar that may be imported by other projects.
Or, even better, use `mvn install` to install the jar in your local maven repository such that other maven projects can
simply find it if added as dependency.

The package phase will also generate the javadoc and a sources jar. The resulting jars cannot be executed as the program
does not provide a main function.

## Usage

This application cannot be used as a standalone application. It uses Spotify's [authorization code flow](https://developer.spotify.com/documentation/web-api/tutorials/code-flow)
which means that it is not possible to authenticate a user without a working (local) server (arguably, it is possible
without a server but highly undesired).

Your server should make use of the `TrueShuffleClient`, which expects a client id, client secret and the redirect uri
to use for the authorization flow. To get a client id and a secret, you must first 
[create an app](https://developer.spotify.com/documentation/web-api/concepts/apps) using the Spotify dashboard. The
redirect URI should be a valid URI that your (local) server can use to process an access token generated by Spotify.
For example, using the following redirect URI `http://localhost:8080/true-shuffle/callback` will make the user redirect
to that URI using a GET request. The authorization may contain the following request parameters: 'code', 'error' and 'state'.
The code can be used to authorize the client (using `TrueShuffleClient#addAuthorizedUser`). After successful authorization,
the client can now be used to shuffle any playlist that is visible to the authenticated user.

## Help

Running into trouble getting your server to work with `TrueShuffleClient`? Feel free to open an issue.

## Bugs

If you find a bug in the program, feel free to open an issue (with reproducible steps).

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE.md) for details.