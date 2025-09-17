document.addEventListener('DOMContentLoaded', () => {
    gsap.registerPlugin(ScrollTrigger);

    // Parallax effect for the background layers
    gsap.to(".layer-1", {
        scrollTrigger: {
            trigger: ".content-container",
            start: "top top",
            end: "bottom top",
            scrub: true,
        },
        y: -150,
        ease: "none"
    });

    gsap.to(".layer-2", {
        scrollTrigger: {
            trigger: ".content-container",
            start: "top top",
            end: "bottom top",
            scrub: true,
        },
        y: -300,
        ease: "none"
    });

    gsap.to(".layer-3", {
        scrollTrigger: {
            trigger: ".content-container",
            start: "top top",
            end: "bottom top",
            scrub: true,
        },
        y: -450,
        ease: "none"
    });

    // Hero section animations
    gsap.from(".main-title", {
        duration: 2,
        opacity: 0,
        y: -50,
        ease: "power2.out",
    });

    gsap.from(".tagline", {
        duration: 1.5,
        opacity: 0,
        delay: 0.5,
        y: 20,
        ease: "power2.out",
    });

    // Animate sections into view
    gsap.utils.toArray("section:not(.hero-section)").forEach(section => {
        gsap.from(section.querySelectorAll("h2, p, .relic-item, .pricing-tier, form"), {
            y: 50,
            opacity: 0,
            stagger: 0.2,
            scrollTrigger: {
                trigger: section,
                start: "top 80%",
                toggleActions: "play none none reverse",
            }
        });
    });

    // Interactive Relic Animations
    gsap.utils.toArray(".relic-item").forEach(item => {
        item.addEventListener('mouseenter', () => {
            gsap.to(item, {
                duration: 0.3,
                scale: 1.1,
                boxShadow: "0 0 30px rgba(255, 0, 0, 0.7)",
                ease: "power2.out"
            });
        });

        item.addEventListener('mouseleave', () => {
            gsap.to(item, {
                duration: 0.3,
                scale: 1,
                boxShadow: "0 0 20px rgba(255, 0, 0, 0.5)",
                ease: "power2.out"
            });
        });
    });

    // Mouse-based tilt for the container
    document.addEventListener('mousemove', (e) => {
        const { clientX, clientY } = e;
        const { innerWidth, innerHeight } = window;
        const xPos = (clientX / innerWidth) - 0.5;
        const yPos = (clientY / innerHeight) - 0.5;
        gsap.to('.content-container', {
            duration: 0.5,
            rotationY: xPos * 5,
            rotationX: -yPos * 5,
            ease: 'power2.out'
        });
    });

    // Randomized, startling animations
    function randomJolt() {
        const joltType = Math.random();
        if (joltType < 0.3) {
            gsap.fromTo('.content-container', { x: 0 }, { duration: 0.1, x: 5, yoyo: true, repeat: 1, ease: 'power1.inOut' });
        } else if (joltType < 0.6) {
            gsap.to('.content-container', { duration: 0.1, backgroundColor: 'rgba(255, 0, 0, 0.1)', yoyo: true, repeat: 1 });
        } else {
            const originalText = document.querySelector('.main-title').innerText;
            document.querySelector('.main-title').innerText = "DIABOLUS";
            setTimeout(() => {
                document.querySelector('.main-title').innerText = originalText;
            }, 100);
        }
    }

    setInterval(randomJolt, Math.random() * (15000 - 10000) + 10000); // 10-15 seconds

    const contactForm = document.getElementById('contact-form');
    if (contactForm) {
        contactForm.addEventListener('submit', function(event) {
            event.preventDefault();
            const name = document.getElementById('name').value;
            const email = document.getElementById('email').value;
            const message = document.getElementById('message').value;
            const subject = 'Lexorcist Contact Form Submission';
            const body = `Name: ${name}%0D%0AEmail: ${email}%0D%0AMessage: ${message}`;
            window.location.href = `mailto:hereliesaz@gmail.com?subject=${encodeURIComponent(subject)}&body=${encodeURIComponent(body)}`;
        });
    }
});