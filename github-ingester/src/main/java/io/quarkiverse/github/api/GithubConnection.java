package io.quarkiverse.github.api;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public interface GithubConnection<T> {
    PageInfo pageInfo();

    List<T> nodes();

    interface InitialQuery<T> {
        GithubConnection<T> query();
    }

    interface PaginateQuery<T> {
        GithubConnection<T> query(String cursor);
    }

    public class IterableConnection<T> implements Iterable<T> {
        private final InitialQuery<T> initialQuery;
        private final PaginateQuery<T> paginateQuery;

        public IterableConnection(InitialQuery<T> initialQuery, PaginateQuery<T> paginateQuery) {
            this.initialQuery = initialQuery;
            this.paginateQuery = paginateQuery;
        }

        class ConnectionIterator implements Iterator<T> {

            private Iterator<T> currentIterator = null;
            private GithubConnection currentConnection = null;

            ConnectionIterator() {
                currentConnection = initialQuery.query();
                currentIterator = currentConnection.nodes().iterator();
            }

            @Override
            public boolean hasNext() {
                return currentIterator.hasNext() || currentConnection.pageInfo().hasNextPage();
            }

            @Override
            public T next() {
                if (currentIterator.hasNext()) {
                    return currentIterator.next();
                } else if (currentConnection.pageInfo().hasNextPage()) {
                    currentConnection = paginateQuery.query(currentConnection.pageInfo().endCursor());
                    currentIterator = currentConnection.nodes().iterator();
                    return currentIterator.next();
                } else {
                    throw new NoSuchElementException();
                }
            }
        }

        @Override
        public Iterator<T> iterator() {
            return new ConnectionIterator();
        }
    }
}
