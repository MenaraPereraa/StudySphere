package lk.menara.studysphere.listener;

public interface FirestoreCallback<T> {
    void onCallback(T data);
}
