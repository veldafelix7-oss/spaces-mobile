package id.veldafelix.spaces.engine;
interface IShell {
    String execute(int operation, String key, String value);
    void destroy();
}
