from faker import Faker

FAKER = Faker()

NUMBER_OF_ENTRIES = 1_000_000


def main():
    nodes = []
    for idx in range(NUMBER_OF_ENTRIES):
        print(f'{idx}: Generating person...')
        nodes.append(f"""
        <person>
            <name>{FAKER.name()}</name>
            <phone>{FAKER.phone_number()}</phone>
            <address>{FAKER.address()}</address>
        </person>
        """)

    root = f"""
    <?xml version="1.0"?>
    <root>
        <metadata>
            <uploaded time="2022-10-09T12:10:00.000Z"/>
        </metadata>
        <data>
            {''.join(nodes)}
        </data>
    </root>
    """.strip()

    with open('./batch_persons.xml', 'w') as f:
        f.write(root)


if __name__ == '__main__':
    main()
