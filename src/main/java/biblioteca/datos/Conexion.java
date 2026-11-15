package biblioteca.datos;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

public class Conexion implements AutoCloseable {
    private final MongoClient cliente;
    private final MongoDatabase base;

    public Conexion() {
        String uri = System.getenv("MONGO_URI");
        if (uri == null || uri.isBlank()) uri = "mongodb://localhost:27017/?serverSelectionTimeoutMS=4000";
        cliente = MongoClients.create(uri);
        base = cliente.getDatabase("biblioteca2026");
        try { base.runCommand(new Document("ping", 1)); }
        catch (RuntimeException e) { cliente.close(); throw e; }
    }

    public MongoDatabase getBase() { return base; }
    @Override public void close() { cliente.close(); }
}
