# Module 1 : OOD

## SOLID principles

### [Single Responsibilty Principle (SRP)](solid-srp.md)

### [Open Closed Principle (OCP)](solid-ocp.md)

### [Liskov Substitution Principle (LSP)](solid-lsp.md)

### [Interface Segregation Principle (ISP)](solid-isp.md)

### [Dependency Inversion Principle (DIP)](solid-dip.md)

---

## OO: Encapsulation & Abstraction

### Information Hiding

#### What it means

- Concealing Details: It involves keeping the inner workings, data structures, and algorithms of a class or module private.
- Public Interface: Only a well-defined public interface (methods) is exposed, through which other parts of the system can interact with the object.
- Separation of Concerns: It helps in separating what an object does (its public behavior) from how it does it (its internal implementation).

#### Why it's important (Benefits)

- Reduced Complexity: By hiding implementation details, users of the object don't need to understand its internal complexity, making the system easier to understand and use.
- Easier Maintenance: If you need to change the internal implementation of an object (e.g., improve an algorithm, change a data structure), you can do so without affecting other parts of the system, as long as the public interface remains the same.
- Improved Robustness: It prevents external code from accidentally or intentionally corrupting an object's internal state. Data is protected and can only be manipulated through the provided methods.
- Better Reusability: Modules with hidden information are often more self-contained and less dependent on external factors, making them easier to reuse in different contexts.
- Team Collaboration: In large projects, different teams can work on different modules concurrently without constantly stepping on each other's toes, as long as they adhere to the public interfaces.

#### How it's achieved

- Access Modifiers: Programming languages use access modifiers (like private, protected in Java/C#, public) to enforce information hiding. private members are hidden from outside classes, while public members form the exposed interface.
- Encapsulation: This is the primary mechanism for achieving information hiding. Encapsulation bundles data (attributes) and the methods that operate on the data into a single unit (a class) and restricts direct access to some of the object's components.
- Abstraction: While closely related, abstraction focuses on showing only essential features and hiding complex background details, often through abstract classes and interfaces. Information hiding is a technique used to achieve abstraction.

**Example:**

Imagine you have a Car object.

- Public Interface (what's exposed): startEngine(), accelerate(), brake(), turnLeft(), turnRight().
- Hidden Information (internal details): The specific type of engine (V6, electric), how the fuel injection system works, the internal mechanics of the transmission, how the anti-lock brakes are implemented.

As a driver, you don't need to know the intricate details of the engine to drive the car. You just need to know how to use the accelerator and brake. If the car manufacturer decides to switch from a gasoline engine to an electric motor, as long as the startEngine(), accelerate(), brake() methods work similarly, you (the "client") don't need to change how you interact with the car. This is information hiding in action.

Information hiding is a cornerstone of good object-oriented design, leading to more modular, maintainable, and robust software systems.

### Data Protection

Data Protection refers to the practice of restricting direct access to an object's internal data (its attributes or state) from outside the object itself. Instead, access is managed through defined interfaces, typically public methods.

Here's a breakdown of what that means and why it's important:

1. Why Data Protection?

Maintaining Data Integrity: It prevents external code from inadvertently or maliciously changing an object's internal state in an invalid way. For example, if you have a BankAccount object, you wouldn't want external code to directly set the balance to a negative number; instead, you'd provide deposit() and withdraw() methods that include validation logic.
Reducing Dependencies (Loose Coupling): When internal data is protected, changes to the object's internal representation don't necessarily affect the code that uses the object, as long as the public interface remains the same. This makes your code more flexible and easier to maintain.
Simplifying Object Usage: Users of an object don't need to know its internal workings. They only need to understand its public interface to interact with it, making the object easier to use and understand.
How is it achieved?

Access Modifiers: This is the primary mechanism. In many object-oriented languages (like Java, C++, C#), keywords like private or protected are used to control the visibility of attributes and methods. Marking an attribute as private means it can only be accessed from within the class itself.
Encapsulation: Data protection is a core aspect of encapsulation. Encapsulation bundles the data (attributes) and the methods (behaviors) that operate on the data into a single unit (a class), and restricts direct access to some of the object's components.
Getters and Setters (Accessor and Mutator Methods): While direct access is restricted, you often need a way for external code to read or sometimes modify the data. This is done through public methods:
Getters (Accessors): Methods that provide read-only access to an attribute (e.g., getBalance()).
Setters (Mutators): Methods that allow controlled modification of an attribute, often including validation logic (e.g., setBalance(amount) which would check if amount is valid).
Example:

Consider a simple Circle class:

Without Data Protection (Bad Practice):

```java
class Circle {
    public double radius; // Public field, anyone can change it directly
    public double calculateArea() {
        return Math.PI * radius * radius;
    }
}
// In another part of the code:
Circle myCircle = new Circle();
myCircle.radius = -5.0; // Oops! A negative radius is invalid.
System.out.println(myCircle.calculateArea()); // Area will be calculated with invalid radius.

```
With Data Protection (Good Practice using Encapsulation):

```java
class Circle {
    private double radius; // Private field, cannot be accessed directly from outside
    public Circle(double radius) {
        if (radius <= 0) {
            throw new IllegalArgumentException("Radius must be positive.");
        }
        this.radius = radius;
    }
    public double getRadius() { // Public getter to read the radius
        return radius;
    }
    public void setRadius(double radius) { // Public setter to modify the radius with validation
        if (radius <= 0) {
            throw new IllegalArgumentException("Radius must be positive.");
        }
        this.radius = radius;
    }
    public double calculateArea() {
        return Math.PI * radius * radius;
    }
}
// In another part of the code:
Circle myCircle = new Circle(10.0);
// myCircle.radius = -5.0; // This would cause a compile-time error!
myCircle.setRadius(12.0); // Valid change
// myCircle.setRadius(-5.0); // This would throw an IllegalArgumentException at runtime, protecting the data.
System.out.println(myCircle.calculateArea());
```
In essence, data protection through encapsulation ensures that objects manage their own state, leading to more robust, maintainable, and understandable code. It's a fundamental principle for good low-level design.

3. Abstract Classes
4. Interfaces
5. Implementation details
6. Cient interaction

## OO: Inheritance vs Composition

## OO: Polymorphism

## OO: Interfaces & Abstract Classes

## Resources

SOLID Principles

1. [OOD Principles](https://www.oodesign.com/design-principles/)
2. [Algomaster - SOLID](https://algomaster.io/learn/lld/srp)
3. [SOLID Principles](https://bool.dev/blog/detail/solid-principles)
4. [OOD Cheatsheet](https://denniskats.dev/resources/pdfs/OOD%20Final.pdf)
5. [SW Cheatsheets](https://github.com/Furkan-Dursun/Software-Cheat-Sheets?tab=readme-ov-file)
