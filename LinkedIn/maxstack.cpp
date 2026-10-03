#include <assert.h>

#include <iostream>
#include <map>
#include <vector>

using namespace std;
int inf = 1e9 + 7;

struct Node {
    int num;
    Node* prev;
    Node* next;

    Node(int num) {
        this->num = num;
        prev = nullptr;
        next = nullptr;
    }
};

struct DoublyLinkedList {
    Node* head;
    Node* tail;

    DoublyLinkedList() {
        head = nullptr;
        tail = nullptr;
    }

    Node* append(int val) {
        Node* newNode = new Node(val);
        if (!tail) {
            head = tail = newNode;
            return head;
        } else {
            tail->next = newNode;
            newNode->prev = tail;
            tail = tail->next;
            return newNode;
        }
    }

    void remove(Node* node) {
        if (!node) {
            return;
        }
        if (node->next != nullptr) {
            node->next->prev = node->prev;
        } else {
            tail = node->prev;
        }
        if (node->prev != nullptr) {
            node->prev->next = node->next;
        } else {
            head = node->next;
        }

        node->next = nullptr;
        node->prev = nullptr;

        delete (node);
    }
};

struct MaxStack {
    map<int, vector<Node*>> treeMap;
    DoublyLinkedList dll;
    int ts;

    MaxStack() { ts = 0; }

    void push(int x) {
        Node* appendNode = dll.append(x);
        treeMap[x].push_back(appendNode);
    }

    int peekMax() {
        int maxx = treeMap.rbegin()->first;
        return maxx;
    }

    int popMax() {
        int maxx = treeMap.rbegin()->first;
        Node* node = treeMap[maxx].back();
        treeMap[maxx].pop_back();
        if (treeMap[maxx].size() == 0) {
            treeMap.erase(maxx);
        }
        dll.remove(node);
        return maxx;
    }

    int pop() {
        if (dll.tail == nullptr) {
            return -inf;
        }
        int x = dll.tail->num;
        Node* tobepopped = dll.tail;
        dll.remove(tobepopped);
        treeMap[x].pop_back();
        if (treeMap[x].empty()) {
            treeMap.erase(x);
        }
        return x;
    }

    int peek() {
        assert(dll.tail != nullptr);
        return dll.tail->num;
    }
};

int main() {}
