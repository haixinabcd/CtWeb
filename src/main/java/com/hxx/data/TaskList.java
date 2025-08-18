package com.hxx.data;

import java.util.Vector;

public class TaskList {
    private   Vector<Task> m_vecTask=new Vector<Task>();
    public void add(Task task){
        m_vecTask.add(task);
    }
    public void remove(Task task){
        m_vecTask.remove(task);
    }
}
