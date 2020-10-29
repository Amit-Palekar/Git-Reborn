# Gitlet Design Document

**Amit Palekar**:

## Classes and Data Structures
### Commit
The class represents a single commit in Git.

#### Fields
1. Directory dir: a directory object containing the changed versions of the files in the commit.

### Directory
This class represents a directory and all subdirectories and files that it contains.

#### Fields
1. Tree dir: a tree representing the contents of the repository. Each subtree within a directory
must either be a directory or a file.

### Branch
This class represents a single branch in Git.

#### Fields
1. ArrayList<Commit> log: contains a list of Commit objects indicating the commit order on the repository.

### Repository
This class represents the entire repository. It has a list of all the branches and knows the master branch.

#### Fields
1. ArrayList<Branch> branches: contains a list of all the branches in the repository.
2. Branch master: points to the master branch.

## Algorithms
### Commit class
1. The class constructor. Will have to be initialized with the state of the repository for that commit.

### Directory class
1. The constructor. It will save the current state of the repository. 

2. add(Directory d): Will add all elements in the directory to the current branch.


### Branch class
1. There must be a merge(Branch b) method. This will take two branches and merge them together.

2. There is also a checkout(Commit c) method. This will checkout the desired branch to the current commit.

3. There will be a log() method that prints out the list of current commits on the branch.

4. There will be a commit(String msg) method that saves commits a current working version of the branch.

5. 

## Persistence
The state of repository is simply just a bunch of data. We can save the state of the repository to a hard disk everytime
a commit or a merge happens (i.e anything that would change the structure of the Git tree 
permanently). However, for memory purposes, we would probably serialize the data first, and then deserialize it to 
retrieve the state of the working tree whenever a user needs to run a command.  

